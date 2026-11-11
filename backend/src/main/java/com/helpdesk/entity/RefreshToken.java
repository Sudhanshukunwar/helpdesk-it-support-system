package com.helpdesk.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

/** Only a SHA-256 hash of the token is stored, so a database leak does not leak usable tokens. */
@Entity
@Table(name = "refresh_tokens")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RefreshToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private User user;

    private LocalDateTime expiresAt;
    private boolean revoked;
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }
}
