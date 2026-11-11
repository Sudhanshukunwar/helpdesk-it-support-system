package com.helpdesk.service;

import com.helpdesk.dto.Dtos.HistoryResponse;
import com.helpdesk.entity.HistoryAction;
import com.helpdesk.entity.Ticket;
import com.helpdesk.entity.TicketHistory;
import com.helpdesk.entity.User;
import com.helpdesk.repository.TicketHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HistoryService {
    private final TicketHistoryRepository repo;

    @Transactional
    public void record(Ticket ticket, User actor, HistoryAction action, String details) {
        repo.save(TicketHistory.builder().ticket(ticket).actor(actor).action(action).details(details).build());
    }

    @Transactional(readOnly = true)
    public List<HistoryResponse> forTicket(Long ticketId) {
        return repo.findByTicketIdOrderByCreatedAtAscIdAsc(ticketId).stream()
                .map(h -> new HistoryResponse(h.getId(), h.getAction(), h.getDetails(),
                        h.getActor() == null ? "System" : h.getActor().getName(), h.getCreatedAt()))
                .toList();
    }
}
