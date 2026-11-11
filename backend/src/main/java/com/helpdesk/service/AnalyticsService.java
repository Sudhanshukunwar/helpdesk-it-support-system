package com.helpdesk.service;

import com.helpdesk.dto.Dtos.AgentLoad;
import com.helpdesk.dto.Dtos.AnalyticsSummary;
import com.helpdesk.entity.*;
import com.helpdesk.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {
    private final TicketRepository tickets;

    @Transactional(readOnly = true)
    public AnalyticsSummary summary() {
        List<Ticket> all = tickets.findAll();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (TicketStatus s : TicketStatus.values()) byStatus.put(s.name(), 0L);
        Map<String, Long> byPriority = new LinkedHashMap<>();
        for (Priority p : Priority.values()) byPriority.put(p.name(), 0L);
        all.forEach(t -> {
            byStatus.merge(t.getStatus().name(), 1L, Long::sum);
            byPriority.merge(t.getPriority().name(), 1L, Long::sum);
        });

        double avgHours = all.stream().filter(t -> t.getResolvedAt() != null)
                .mapToDouble(t -> ChronoUnit.MINUTES.between(t.getCreatedAt(), t.getResolvedAt()) / 60.0)
                .average().orElse(0);

        long breached = all.stream().filter(Ticket::isSlaBreached).count();
        double breachPct = all.isEmpty() ? 0 : breached * 100.0 / all.size();

        var load = all.stream().filter(t -> t.getAssignedTo() != null)
                .collect(Collectors.groupingBy(t -> t.getAssignedTo().getName(), TreeMap::new, Collectors.toList()))
                .entrySet().stream()
                .map(e -> new AgentLoad(e.getKey(),
                        e.getValue().stream().filter(t -> t.getStatus() == TicketStatus.OPEN
                                || t.getStatus() == TicketStatus.IN_PROGRESS).count(),
                        e.getValue().stream().filter(t -> t.getStatus() == TicketStatus.RESOLVED
                                || t.getStatus() == TicketStatus.CLOSED).count()))
                .toList();

        return new AnalyticsSummary(all.size(), byStatus, byPriority,
                Math.round(avgHours * 10) / 10.0, Math.round(breachPct * 10) / 10.0, load);
    }
}
