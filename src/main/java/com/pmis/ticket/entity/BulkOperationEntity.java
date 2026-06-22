package com.pmis.ticket.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "pmis_ticket_bulk_operation")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class BulkOperationEntity {

    @Id
    @Column(name = "uuid", nullable = false, length = 64)
    private String uuid;

    /** STATUS_UPDATE | ASSIGNMENT | CLOSE */
    @Column(name = "operation_type", nullable = false, length = 32)
    private String operationType;

    @Column(name = "ticket_uuids", columnDefinition = "text[]")
    @JdbcTypeCode(SqlTypes.ARRAY)
    private String[] ticketUuids;

    /** JSON payload e.g. {newStatus, comment, assigneeUuid} */
    @Column(name = "payload", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String payload;

    @Column(name = "total_count", nullable = false)
    private Integer totalCount;

    @Column(name = "success_count")
    private Integer successCount = 0;

    @Column(name = "failed_count")
    private Integer failedCount = 0;

    /** PENDING | PROCESSING | DONE | FAILED */
    @Column(name = "status", nullable = false, length = 16)
    private String status = "PENDING";

    @Column(name = "performed_by", nullable = false, length = 64)
    private String performedBy;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "completed_at")
    private Long completedAt;
}
