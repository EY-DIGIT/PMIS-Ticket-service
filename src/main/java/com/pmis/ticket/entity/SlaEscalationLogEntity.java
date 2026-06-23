package com.pmis.ticket.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pmis_sla_escalation_log",
       indexes = @Index(name = "idx_escalation_ticket_level",
                        columnList = "ticket_uuid, escalation_level"))
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SlaEscalationLogEntity {

    @Id
    @Column(name = "uuid", nullable = false, length = 64)
    private String uuid;

    @Column(name = "ticket_uuid", nullable = false, length = 64)
    private String ticketUuid;

    /**
     * Escalation milestone triggered:
     *   RISK_50        — 50 % of SLA consumed
     *   RISK_75        — 75 % of SLA consumed
     *   BREACH         — resolution SLA deadline passed
     *   FIRST_RESPONSE — first-response SLA deadline passed
     *   POST_BREACH_24H — 24 h after resolution breach
     */
    @Column(name = "escalation_level", nullable = false, length = 32)
    private String escalationLevel;

    /** JSON array of role codes that were notified: ["MANAGER","LEAD"] */
    @Column(name = "notified_roles", columnDefinition = "text")
    private String notifiedRoles;

    @Column(name = "escalated_at", nullable = false)
    private Long escalatedAt;
}
