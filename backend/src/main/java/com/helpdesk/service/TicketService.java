package com.helpdesk.service;

import com.helpdesk.dto.Dtos.*;
import com.helpdesk.entity.*;
import com.helpdesk.exception.BadRequestException;
import com.helpdesk.exception.NotFoundException;
import com.helpdesk.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional
public class TicketService {
    private final TicketRepository tickets;
    private final UserRepository users;
    private final CommentRepository comments;
    private final SlaPolicyRepository slaRepo;
    private final NotificationService notifications;

    // ---------- helpers ----------
    private User current(String email) {
        return users.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
    }

    private Ticket find(Long id) {
        return tickets.findById(id).orElseThrow(() -> new NotFoundException("Ticket not found"));
    }

    private void checkAccess(Ticket t, User u) {
        switch (u.getRole()) {
            case ADMIN -> { }
            case EMPLOYEE -> {
                if (!t.getCreatedBy().getId().equals(u.getId()))
                    throw new AccessDeniedException("Not your ticket");
            }
            case SUPPORT -> {
                if (t.getAssignedTo() != null && !t.getAssignedTo().getId().equals(u.getId()))
                    throw new AccessDeniedException("Assigned to another agent");
            }
        }
    }

    /** Notify the creator and the assignee, except the person who did the action. */
    private void notifyParties(Ticket t, User actor, String message) {
        if (t.getCreatedBy() != null && !Objects.equals(t.getCreatedBy().getId(), actor.getId()))
            notifications.notify(t.getCreatedBy(), message, t.getId());
        if (t.getAssignedTo() != null && !Objects.equals(t.getAssignedTo().getId(), actor.getId())
                && !Objects.equals(t.getAssignedTo().getId(), t.getCreatedBy().getId()))
            notifications.notify(t.getAssignedTo(), message, t.getId());
    }

    private TicketResponse toDto(Ticket t) {
        var c = t.getCreatedBy();
        var a = t.getAssignedTo();
        return new TicketResponse(t.getId(), t.getTitle(), t.getDescription(), t.getCategory(),
                t.getStatus(), t.getPriority(),
                c.getId(), c.getName(),
                a == null ? null : a.getId(), a == null ? null : a.getName(),
                t.getCreatedAt(), t.getDueAt(), t.getResolvedAt(), t.isSlaBreached());
    }

    // ---------- tickets ----------
    public TicketResponse create(CreateTicketRequest r, String email) {
        var user = current(email);
        int hours = slaRepo.findById(r.priority()).map(SlaPolicy::getResolutionHours).orElse(24);
        var t = tickets.save(Ticket.builder()
                .title(r.title().trim()).description(r.description()).category(r.category())
                .status(TicketStatus.OPEN).priority(r.priority())
                .createdBy(user).dueAt(LocalDateTime.now().plusHours(hours)).build());
        notifications.notifyRole(Role.ADMIN, "New " + t.getPriority() + " ticket #" + t.getId() + ": " + t.getTitle(), t.getId());
        return toDto(t);
    }

    @Transactional(readOnly = true)
    public List<TicketResponse> list(String email) {
        var u = current(email);
        List<Ticket> result = switch (u.getRole()) {
            case ADMIN -> tickets.findAllByOrderByCreatedAtDesc();
            case EMPLOYEE -> tickets.findByCreatedByIdOrderByCreatedAtDesc(u.getId());
            case SUPPORT -> tickets.findVisibleToSupport(u.getId());
        };
        return result.stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public TicketResponse get(Long id, String email) {
        var t = find(id);
        checkAccess(t, current(email));
        return toDto(t);
    }

    public TicketResponse assign(Long id, Long assigneeId, String email) {
        var actor = current(email);
        var t = find(id);
        var agent = users.findById(assigneeId).orElseThrow(() -> new NotFoundException("Assignee not found"));
        if (agent.getRole() != Role.SUPPORT) throw new BadRequestException("Assignee must be a support agent");
        t.setAssignedTo(agent);
        if (t.getStatus() == TicketStatus.OPEN) t.setStatus(TicketStatus.IN_PROGRESS);
        notifications.notify(agent, "Ticket #" + t.getId() + " assigned to you: " + t.getTitle(), t.getId());
        notifyParties(t, actor, "Ticket #" + t.getId() + " was assigned to " + agent.getName());
        return toDto(t);
    }

    public TicketResponse claim(Long id, String email) {
        var agent = current(email);
        var t = find(id);
        if (t.getAssignedTo() != null) throw new BadRequestException("Ticket is already assigned");
        t.setAssignedTo(agent);
        if (t.getStatus() == TicketStatus.OPEN) t.setStatus(TicketStatus.IN_PROGRESS);
        notifyParties(t, agent, agent.getName() + " picked up ticket #" + t.getId());
        return toDto(t);
    }

    public TicketResponse updateStatus(Long id, TicketStatus status, String email) {
        var u = current(email);
        var t = find(id);
        checkAccess(t, u);
        if (u.getRole() == Role.EMPLOYEE && status != TicketStatus.CLOSED)
            throw new AccessDeniedException("Employees can only close their own tickets");
        if (u.getRole() == Role.SUPPORT && t.getAssignedTo() == null)
            throw new BadRequestException("Claim the ticket before changing its status");
        t.setStatus(status);
        if (status == TicketStatus.RESOLVED || status == TicketStatus.CLOSED) {
            if (t.getResolvedAt() == null) t.setResolvedAt(LocalDateTime.now());
        } else {
            t.setResolvedAt(null);
        }
        notifyParties(t, u, "Ticket #" + t.getId() + " is now " + status.name().replace('_', ' '));
        return toDto(t);
    }

    // ---------- comments ----------
    public CommentResponse addComment(Long id, String message, String email) {
        var u = current(email);
        var t = find(id);
        checkAccess(t, u);
        var c = comments.save(Comment.builder().ticket(t).user(u).message(message.trim()).build());
        notifyParties(t, u, u.getName() + " commented on ticket #" + t.getId());
        return toDto(c);
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> comments(Long id, String email) {
        var t = find(id);
        checkAccess(t, current(email));
        return comments.findByTicketIdOrderByCreatedAtAsc(id).stream().map(this::toDto).toList();
    }

    private CommentResponse toDto(Comment c) {
        return new CommentResponse(c.getId(), c.getUser().getId(), c.getUser().getName(),
                c.getUser().getRole(), c.getMessage(), c.getCreatedAt());
    }
}
