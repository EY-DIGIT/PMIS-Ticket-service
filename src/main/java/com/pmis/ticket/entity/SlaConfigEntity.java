package com.pmis.ticket.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pmis_ticket_sla_config",
       uniqueConstraints = @UniqueConstraint(columnNames = {"category", "priority"}))
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SlaConfigEntity {

    @Id
    @Column(name = "uuid", nullable = false, length = 64)
    private String uuid;

    @Column(name = "category", nullable = false, length = 32)
    private String category;

    @Column(name = "priority", nullable = false, length = 16)
    private String priority;

    /** SLA in hours for resolution. */
    @Column(name = "sla_hours", nullable = false)
    private Integer slaHours;

    /** Escalate if not assigned within N hours. */
    @Column(name = "escalation_hours")
    private Integer escalationHours;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "updated_at")
    private Long updatedAt;
}
