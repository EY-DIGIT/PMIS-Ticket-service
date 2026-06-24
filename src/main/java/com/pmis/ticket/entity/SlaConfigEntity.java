package com.pmis.ticket.entity;

import jakarta.persistence.*;
import lombok.*;

/** One row = one SLA rule for a (category, priority) combination. */
@Entity
@Table(name = "pmis_ticket_sla_config")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SlaConfigEntity {

    @Id
    @Column(name = "uuid", nullable = false, length = 64)
    private String uuid;

    /** INCIDENT | SERVICE_REQUEST | CHANGE | PROBLEM */
    @Column(name = "category", nullable = false, length = 32)
    private String category;

    /** CRITICAL | HIGH | MEDIUM | LOW */
    @Column(name = "priority", nullable = false, length = 16)
    private String priority;

    /** Hours until first response is required. Nullable — not all legacy configs have this set. */
    @Column(name = "first_response_hours")
    private Integer firstResponseHours;

    /** Hours until the ticket must be fully resolved. */
    @Column(name = "sla_hours", nullable = false)
    private Integer slaHours;

    /**
     * BUSINESS_HOURS – only count Mon–Fri 09:00–18:00 (per working calendar).
     * CALENDAR_HOURS – count 24 × 7 wall-clock hours (CRITICAL tickets).
     *
     * columnDefinition includes DEFAULT so Hibernate's ALTER TABLE ADD COLUMN
     * back-fills existing rows instead of failing with "contains null values".
     */
    @Builder.Default
    @Column(name = "clock_type", nullable = false,
            columnDefinition = "varchar(16) default 'BUSINESS_HOURS'")
    private String clockType = "BUSINESS_HOURS";

    /** JSON array of role codes notified at 50 % SLA consumption: ["LEAD","MANAGER"] */
    @Column(name = "escalation_50_pct_roles", columnDefinition = "text")
    private String escalation50PctRoles;

    /** JSON array of role codes notified at 75 % SLA consumption. */
    @Column(name = "escalation_75_pct_roles", columnDefinition = "text")
    private String escalation75PctRoles;

    /** JSON array of role codes notified on SLA breach. */
    @Column(name = "escalation_breach_roles", columnDefinition = "text")
    private String escalationBreachRoles;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "updated_at")
    private Long updatedAt;
}
