package com.pmis.ticket.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pmis_ticket_document")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class TicketDocumentEntity {

    @Id
    @Column(name = "uuid", nullable = false, length = 64)
    private String uuid;

    @Column(name = "ticket_uuid", nullable = false, length = 64)
    private String ticketUuid;

    /** Original filename as uploaded by the user */
    @Column(name = "original_name", nullable = false, length = 512)
    private String originalName;

    /** Name under which the file is physically stored on disk: {uuid}_{originalName} */
    @Column(name = "stored_name", nullable = false, length = 512)
    private String storedName;

    /** Absolute path to the file on the local filesystem */
    @Column(name = "file_path", nullable = false, columnDefinition = "text")
    private String filePath;

    @Column(name = "mime_type", length = 128)
    private String mimeType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "uploaded_by_uuid", length = 64)
    private String uploadedByUuid;

    @Column(name = "uploaded_by_name", length = 256)
    private String uploadedByName;

    @Column(name = "uploaded_at", nullable = false)
    private Long uploadedAt;
}
