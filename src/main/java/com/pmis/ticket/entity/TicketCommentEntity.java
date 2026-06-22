package com.pmis.ticket.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pmis_ticket_comment")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TicketCommentEntity {

    @Id
    @Column(name = "uuid", nullable = false, length = 64)
    private String uuid;

    @Column(name = "ticket_uuid", nullable = false, length = 64)
    private String ticketUuid;

    /** COMMENT | STATUS_CHANGE | ASSIGNMENT | SYSTEM */
    @Column(name = "comment_type", nullable = false, length = 16)
    private String commentType;

    @Column(name = "body", columnDefinition = "text")
    private String body;

    @Column(name = "previous_status", length = 32)
    private String previousStatus;

    @Column(name = "new_status", length = 32)
    private String newStatus;

    @Column(name = "previous_assignee", length = 64)
    private String previousAssignee;

    @Column(name = "new_assignee", length = 64)
    private String newAssignee;

    @Column(name = "author_uuid", nullable = false, length = 64)
    private String authorUuid;

    @Column(name = "author_name", length = 256)
    private String authorName;

    @Column(name = "author_email", length = 256)
    private String authorEmail;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;
}
