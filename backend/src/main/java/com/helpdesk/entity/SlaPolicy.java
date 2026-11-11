package com.helpdesk.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sla_policies")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SlaPolicy {
    @Id @Enumerated(EnumType.STRING)
    private Priority priority;
    private int responseHours;
    private int resolutionHours;
}
