package com.helpdesk.scheduler;

import com.helpdesk.entity.*;
import com.helpdesk.repository.TicketRepository;
import com.helpdesk.service.HistoryService;
import com.helpdesk.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SlaSchedulerTest {
    @Mock TicketRepository tickets;
    @Mock NotificationService notifications;
    @Mock HistoryService history;
    @InjectMocks SlaScheduler scheduler;

    @Test
    void overdueTicketIsFlaggedLoggedAndEscalated() {
        var creator = User.builder().id(1L).name("Emp").email("e@x.com").password("h").role(Role.EMPLOYEE).build();
        var t = Ticket.builder().id(7L).title("Laptop").priority(Priority.CRITICAL).status(TicketStatus.OPEN)
                .createdBy(creator).dueAt(LocalDateTime.now().minusMinutes(5)).build();
        when(tickets.findByStatusInAndSlaBreachedFalseAndDueAtBefore(any(), any())).thenReturn(List.of(t));

        scheduler.escalateBreached();

        assertTrue(t.isSlaBreached());
        verify(history).record(eq(t), isNull(), eq(HistoryAction.SLA_BREACHED), anyString());
        verify(notifications).notifyRole(eq(Role.ADMIN), anyString(), eq(7L));
        verify(notifications).notifyRole(eq(Role.SUPPORT), anyString(), eq(7L)); // unassigned: all agents are told
    }
}
