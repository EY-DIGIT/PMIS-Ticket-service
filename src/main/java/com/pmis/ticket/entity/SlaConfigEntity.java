package com.pmis.ticket.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * One row = one SLA rule for a (tenantId, category, priority) combination.
 * tenantId = null  →  global default rule (applies to all tenants without an override).
 * tenantId = "XYZ" →  tenant-specific override; takes priority over the global rule.
 *
 * DB note: enforce uniqueness per (category, priority) for global rows and per
 * (tenant_id, category, priority) for tenant rows using partial unique indexes —
 * JPA @UniqueConstraint cannot express nullable-column partitioning.
 */
@Entity
@Table(name = "pmis_ticket_sla_config")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SlaConfigEntity {

    @Id
    @Column(name = "uuid", nullable = false, length = 64)
    private String uuid;

    /** null = global default; non-null = tenant-specific override */
    @Column(name = "tenant_id", length = 64)
    private String tenantId;

    /** INCIDENT | SERVICE_REQUEST | CHANGE | PROBLEM */
    @Column(name = "category", nullable = false, length = 32)
    private String category;

    /** CRITICAL | HIGH | MEDIUM | LOW */
    @Column(name = "priority", nullable = false, length = 16)
    private String priority;

    /** Hours until first response is required. */
    @Column(name = "first_response_hours", nullable = false)
    private Integer firstResponseHours;

    /** Hours until the ticket must be fully resolved. */
    @Column(name = "sla_hours", nullable = false)
    private Integer slaHours;

    /**
     * BUSINESS_HOURS – only count Mon–Fri 09:00–18:00 (per working calendar).
     * CALENDAR_HOURS – count 24 × 7 wall-clock hours (CRITICAL tickets).
     */
    @Column(name = "clock_type", nullable = false, length = 16)
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

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "updated_at")
    private Long updatedAt;
}
