package com.helpdesk.repository;

import com.helpdesk.entity.Ticket;
import com.helpdesk.entity.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface TicketRepository extends JpaRepository<Ticket, Long> {
    List<Ticket> findAllByOrderByCreatedAtDesc();
    List<Ticket> findByCreatedByIdOrderByCreatedAtDesc(Long userId);

    @Query("select t from Ticket t where t.assignedTo is null or t.assignedTo.id = :id order by t.createdAt desc")
    List<Ticket> findVisibleToSupport(@Param("id") Long id);

    List<Ticket> findByStatusInAndSlaBreachedFalseAndDueAtBefore(Collection<TicketStatus> statuses, LocalDateTime now);
}
