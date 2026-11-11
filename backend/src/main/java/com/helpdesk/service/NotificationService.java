package com.helpdesk.service;

import com.helpdesk.dto.Dtos.NotificationResponse;
import com.helpdesk.entity.Notification;
import com.helpdesk.entity.Role;
import com.helpdesk.entity.User;
import com.helpdesk.exception.NotFoundException;
import com.helpdesk.repository.NotificationRepository;
import com.helpdesk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {
    private final NotificationRepository repo;
    private final UserRepository users;

    public void notify(User user, String message, Long ticketId) {
        repo.save(Notification.builder().user(user).message(message).ticketId(ticketId).build());
    }

    public void notifyRole(Role role, String message, Long ticketId) {
        users.findByRole(role).forEach(u -> notify(u, message, ticketId));
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(String email) {
        var u = users.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        return repo.findTop50ByUserIdOrderByCreatedAtDesc(u.getId()).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public long unreadCount(String email) {
        var u = users.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        return repo.countByUserIdAndSeenFalse(u.getId());
    }

    public void markRead(Long id, String email) {
        var n = repo.findById(id).orElseThrow(() -> new NotFoundException("Notification not found"));
        if (!n.getUser().getEmail().equals(email)) throw new AccessDeniedException("Not your notification");
        n.setSeen(true);
    }

    public void markAllRead(String email) {
        var u = users.findByEmail(email).orElseThrow(() -> new NotFoundException("User not found"));
        repo.findByUserIdAndSeenFalse(u.getId()).forEach(n -> n.setSeen(true));
    }

    private NotificationResponse toDto(Notification n) {
        return new NotificationResponse(n.getId(), n.getMessage(), n.getTicketId(), n.isSeen(), n.getCreatedAt());
    }
}
