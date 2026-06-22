package com.pmis.ticket.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pmis_ticket_attachment")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TicketAttachmentEntity {

    @Id
    @Column(name = "uuid", nullable = false, length = 64)
    private String uuid;

    @Column(name = "ticket_uuid", nullable = false, length = 64)
    private String ticketUuid;

    @Column(name = "comment_uuid", length = 64)
    private String commentUuid;

    @Column(name = "file_name", nullable = false, length = 512)
    private String fileName;

    @Column(name = "mime_type", length = 128)
    private String mimeType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "file_url", nullable = false, columnDefinition = "text")
    private String fileUrl;

    @Column(name = "uploaded_by_uuid", length = 64)
    private String uploadedByUuid;

    @Column(name = "uploaded_at", nullable = false)
    private Long uploadedAt;
}
