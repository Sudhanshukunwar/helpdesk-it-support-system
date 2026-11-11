package com.helpdesk.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Append-only audit trail of what happened to a ticket. */
@Entity
@Table(name = "ticket_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TicketHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Ticket ticket;

    /** Null when the system did it (for example an SLA breach). */
    @ManyToOne(fetch = FetchType.LAZY)
    private User actor;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false)
    private HistoryAction action;

    @Column(length = 500)
    private String details;

    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }
}
