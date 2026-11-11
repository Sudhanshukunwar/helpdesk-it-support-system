package com.helpdesk.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Builder.Default
    private boolean active = true;

    /** Consecutive wrong-password attempts since the last success or lock. */
    @Column(columnDefinition = "int default 0")
    private int failedAttempts;

    /** Login is refused until this time. */
    private LocalDateTime lockedUntil;

    /** Part of every access token. Bumping it invalidates all tokens issued before. */
    @Column(columnDefinition = "int default 0")
    private int tokenVersion;
}
