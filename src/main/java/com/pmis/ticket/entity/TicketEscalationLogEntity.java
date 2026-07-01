package com.pmis.ticket.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "pmis_ticket_escalation_log",
       uniqueConstraints = @UniqueConstraint(columnNames = {"ticket_uuid", "level"}))
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TicketEscalationLogEntity {

    @Id
    @Column(name = "uuid", nullable = false, length = 64)
    private String uuid;

    @Column(name = "ticket_uuid", nullable = false, length = 64)
    private String ticketUuid;

    @Column(name = "ticket_number", length = 64)
    private String ticketNumber;

    @Column(name = "priority", nullable = false, length = 8)
    private String priority;        // P1 | P2 | P3

    @Column(name = "level", nullable = false, length = 4)
    private String level;           // L1 | L2 | L3

    @Column(name = "trigger_hours", nullable = false)
    private Integer triggerHours;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "emails_sent", columnDefinition = "text", nullable = false)
    private List<String> emailsSent;

    @Column(name = "triggered_at", nullable = false)
    private Long triggeredAt;       // epoch ms when escalation fired
}
