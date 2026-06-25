package com.pmis.ticket.web.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TicketDocumentResponse {
    private String uuid;
    private String ticketUuid;
    private String originalName;
    private String storedName;
    private String filePath;
    private String mimeType;
    private Long   sizeBytes;
    private String uploadedByUuid;
    private String uploadedByName;
    private Long   uploadedAt;
}
