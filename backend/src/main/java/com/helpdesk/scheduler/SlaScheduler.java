package com.helpdesk.scheduler;

import com.helpdesk.entity.HistoryAction;
import com.helpdesk.entity.Role;
import com.helpdesk.entity.TicketStatus;
import com.helpdesk.repository.TicketRepository;
import com.helpdesk.service.HistoryService;
import com.helpdesk.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/** Flags tickets that passed their SLA due time and escalates to admins (and the assignee, if any). */
@Component
@RequiredArgsConstructor
public class SlaScheduler {
    private final TicketRepository tickets;
    private final NotificationService notifications;
    private final HistoryService history;

    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void escalateBreached() {
        var overdue = tickets.findByStatusInAndSlaBreachedFalseAndDueAtBefore(
                List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS), LocalDateTime.now());
        for (var t : overdue) {
            t.setSlaBreached(true);
            history.record(t, null, HistoryAction.SLA_BREACHED, "SLA due time passed");
            String msg = "SLA breached: ticket #" + t.getId() + " (" + t.getPriority() + ") " + t.getTitle();
            notifications.notifyRole(Role.ADMIN, msg, t.getId());
            if (t.getAssignedTo() != null) notifications.notify(t.getAssignedTo(), msg, t.getId());
            else notifications.notifyRole(Role.SUPPORT, msg + " is still unassigned", t.getId());
        }
    }
}
