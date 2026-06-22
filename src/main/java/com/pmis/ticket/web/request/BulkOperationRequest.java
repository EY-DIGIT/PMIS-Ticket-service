package com.pmis.ticket.web.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BulkOperationRequest {
    private RequestInfo requestInfo;
    private BulkOp operation;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BulkOp {
        private String type;            // STATUS_UPDATE | ASSIGNMENT | CLOSE
        private List<String> ticketUuids;
        private BulkPayload payload;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BulkPayload {
        private String newStatus;
        private String assigneeUuid;
        private String assigneeName;
        private String assigneeEmail;
        private String comment;
    }
}
