package com.pmis.ticket.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "pmis_ticket")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TicketEntity {

    @Id
    @Column(name = "uuid", nullable = false, length = 64)
    private String uuid;

    @Column(name = "ticket_number", nullable = false, unique = true, length = 64)
    private String ticketNumber;

    // ---- category (FR-36) ----
    @Column(name = "category", nullable = false, length = 32)
    private String category;         // INCIDENT | SERVICE_REQUEST | CHANGE | PROBLEM

    @Column(name = "sub_category", length = 128)
    private String subCategory;

    // ---- priority / core (FR-37) ----
    @Column(name = "priority", nullable = false, length = 16)
    private String priority;         // CRITICAL | HIGH | MEDIUM | LOW

    @Column(name = "title", nullable = false, length = 512)
    private String title;

    @Column(name = "description", columnDefinition = "text")
    private String description;

    @Column(name = "status", nullable = false, length = 32)
    private String status;           // OPEN | IN_PROGRESS | PENDING | RESOLVED | CLOSED | CANCELLED

    // ---- linkage (FR-35.1) ----
    @Column(name = "project_id", length = 64)
    private String projectId;

    @Column(name = "activity_id", length = 64)
    private String activityId;

    @Column(name = "task_id", length = 64)
    private String taskId;

    // ---- parent-child (FR-35.4) ----
    @Column(name = "parent_ticket_uuid", length = 64)
    private String parentTicketUuid;

    // ---- assignment (FR-37.3) ----
    @Column(name = "assignee_uuid", length = 64)
    private String assigneeUuid;

    @Column(name = "assignee_name", length = 256)
    private String assigneeName;

    @Column(name = "assignee_email", length = 256)
    private String assigneeEmail;

    // ---- SLA — resolution ----
    @Column(name = "sla_deadline")
    private Long slaDeadline;

    @Builder.Default
    @Column(name = "sla_breached", nullable = false)
    private Boolean slaBreached = false;

    @Column(name = "sla_breached_at")
    private Long slaBreachedAt;

    // ---- SLA — first response ----
    /** Epoch ms by which the ticket must receive a first response (assignee set). */
    @Column(name = "first_response_deadline")
    private Long firstResponseDeadline;

    /** True once the first-response deadline passes without a response recorded. */
    @Builder.Default
    @Column(name = "first_response_breached", nullable = false,
            columnDefinition = "boolean default false")
    private Boolean firstResponseBreached = false;

    /** Epoch ms when a non-reporter user first responded (assignee set / comment added). */
    @Column(name = "first_response_at")
    private Long firstResponseAt;

    // ---- SLA — pause / resume ----
    /** Epoch ms when the SLA clock was last paused (ticket → PENDING). null = running. */
    @Column(name = "sla_paused_at")
    private Long slaPausedAt;

    /** Total accumulated paused duration in ms across all PENDING periods. */
    @Builder.Default
    @Column(name = "total_paused_ms", nullable = false,
            columnDefinition = "bigint default 0")
    private Long totalPausedMs = 0L;

    // ---- SLA — composite status ----
    /** ON_TRACK | AT_RISK_50 | AT_RISK_75 | BREACHED | PAUSED | NO_SLA */
    @Builder.Default
    @Column(name = "sla_status", nullable = false,
            columnDefinition = "varchar(16) default 'ON_TRACK'")
    private String slaStatus = "ON_TRACK";

    // ---- Change ticket extras (FR-36.3) ----
    @Column(name = "baseline_ref", length = 128)
    private String baselineRef;

    @Column(name = "contract_ref", length = 128)
    private String contractRef;

    // ---- reporter ----
    @Column(name = "reported_by_uuid", nullable = false, length = 64)
    private String reportedByUuid;

    @Column(name = "reported_by_name", length = 256)
    private String reportedByName;

    @Column(name = "reported_by_email", length = 256)
    private String reportedByEmail;

    // ---- audit ----
    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "updated_at")
    private Long updatedAt;

    @Column(name = "resolved_at")
    private Long resolvedAt;

    @Column(name = "closed_at")
    private Long closedAt;
}
