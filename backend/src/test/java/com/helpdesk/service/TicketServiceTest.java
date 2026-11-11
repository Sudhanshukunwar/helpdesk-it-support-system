package com.helpdesk.service;

import com.helpdesk.dto.Dtos.CreateTicketRequest;
import com.helpdesk.dto.Dtos.TicketResponse;
import com.helpdesk.entity.*;
import com.helpdesk.exception.BadRequestException;
import com.helpdesk.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TicketServiceTest {
    @Mock TicketRepository tickets;
    @Mock UserRepository users;
    @Mock CommentRepository comments;
    @Mock SlaPolicyRepository slaRepo;
    @Mock NotificationService notifications;
    @Mock HistoryService history;
    @InjectMocks TicketService service;

    User employee, stranger, agent, otherAgent, admin;
    Ticket ticket;

    private static User user(long id, String email, Role role) {
        return User.builder().id(id).name(email.split("@")[0]).email(email).password("h").role(role).build();
    }

    @BeforeEach
    void setUp() {
        employee = user(1L, "emp@x.com", Role.EMPLOYEE);
        stranger = user(2L, "stranger@x.com", Role.EMPLOYEE);
        agent = user(3L, "agent@x.com", Role.SUPPORT);
        otherAgent = user(4L, "agent2@x.com", Role.SUPPORT);
        admin = user(5L, "admin@x.com", Role.ADMIN);
        for (User u : new User[]{employee, stranger, agent, otherAgent, admin}) {
            when(users.findByEmail(u.getEmail())).thenReturn(Optional.of(u));
            when(users.findById(u.getId())).thenReturn(Optional.of(u));
        }
        ticket = Ticket.builder().id(10L).title("VPN down").status(TicketStatus.OPEN)
                .priority(Priority.HIGH).createdBy(employee).build();
        when(tickets.findById(10L)).thenReturn(Optional.of(ticket));
        when(tickets.save(any(Ticket.class))).thenAnswer(i -> i.getArgument(0));
    }

    // ----- access rules -----
    @Test
    void employeeCanOpenTheirOwnTicket() {
        assertEquals(10L, service.get(10L, "emp@x.com").id());
    }

    @Test
    void employeeCannotOpenSomeoneElsesTicket() {
        assertThrows(AccessDeniedException.class, () -> service.get(10L, "stranger@x.com"));
    }

    @Test
    void supportCannotOpenATicketAssignedToAnotherAgent() {
        ticket.setAssignedTo(otherAgent);
        assertThrows(AccessDeniedException.class, () -> service.get(10L, "agent@x.com"));
    }

    @Test
    void adminCanOpenAnyTicket() {
        assertEquals(10L, service.get(10L, "admin@x.com").id());
    }

    // ----- creating -----
    @Test
    void newTicketGetsItsDueTimeFromTheSlaPolicy() {
        when(slaRepo.findById(Priority.HIGH)).thenReturn(Optional.of(new SlaPolicy(Priority.HIGH, 2, 8)));
        LocalDateTime before = LocalDateTime.now();
        TicketResponse r = service.create(new CreateTicketRequest("Printer", "jammed", "Hardware", Priority.HIGH), "emp@x.com");
        LocalDateTime after = LocalDateTime.now();
        assertEquals(TicketStatus.OPEN, r.status());
        assertFalse(r.dueAt().isBefore(before.plusHours(8)));
        assertFalse(r.dueAt().isAfter(after.plusHours(8)));
    }

    // ----- status rules -----
    @Test
    void employeeCanOnlyCloseTheirTicket() {
        assertThrows(AccessDeniedException.class, () -> service.updateStatus(10L, TicketStatus.RESOLVED, "emp@x.com"));
        TicketResponse r = service.updateStatus(10L, TicketStatus.CLOSED, "emp@x.com");
        assertEquals(TicketStatus.CLOSED, r.status());
        assertNotNull(r.resolvedAt());
    }

    @Test
    void supportMustPickUpATicketBeforeChangingItsStatus() {
        assertThrows(BadRequestException.class, () -> service.updateStatus(10L, TicketStatus.IN_PROGRESS, "agent@x.com"));
    }

    @Test
    void resolvingSetsResolvedAtAndWritesAHistoryEntry() {
        ticket.setAssignedTo(agent);
        ticket.setStatus(TicketStatus.IN_PROGRESS);
        TicketResponse r = service.updateStatus(10L, TicketStatus.RESOLVED, "agent@x.com");
        assertNotNull(r.resolvedAt());
        verify(history).record(eq(ticket), eq(agent), eq(HistoryAction.STATUS_CHANGED), contains("Resolved"));
    }

    @Test
    void reopeningATicketClearsResolvedAt() {
        ticket.setAssignedTo(agent);
        ticket.setStatus(TicketStatus.RESOLVED);
        ticket.setResolvedAt(LocalDateTime.now());
        TicketResponse r = service.updateStatus(10L, TicketStatus.IN_PROGRESS, "agent@x.com");
        assertNull(r.resolvedAt());
    }

    @Test
    void settingTheSameStatusAgainDoesNothing() {
        ticket.setAssignedTo(agent);
        service.updateStatus(10L, TicketStatus.OPEN, "agent@x.com");
        verify(history, never()).record(any(), any(), any(), anyString());
    }

    // ----- assignment -----
    @Test
    void ticketsCanOnlyBeAssignedToSupportAgents() {
        assertThrows(BadRequestException.class, () -> service.assign(10L, 2L, "admin@x.com"));
    }

    @Test
    void assigningAnOpenTicketMovesItToInProgressAndLogsBothSteps() {
        TicketResponse r = service.assign(10L, 3L, "admin@x.com");
        assertEquals(TicketStatus.IN_PROGRESS, r.status());
        assertEquals(3L, r.assignedToId());
        verify(history).record(eq(ticket), eq(admin), eq(HistoryAction.ASSIGNED), contains("Assigned to"));
        verify(history).record(eq(ticket), eq(admin), eq(HistoryAction.STATUS_CHANGED), contains("In progress"));
    }

    @Test
    void agentCannotPickUpAnAlreadyAssignedTicket() {
        ticket.setAssignedTo(otherAgent);
        assertThrows(BadRequestException.class, () -> service.claim(10L, "agent@x.com"));
    }
}
