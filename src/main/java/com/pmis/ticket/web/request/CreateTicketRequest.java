package com.pmis.ticket.web.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateTicketRequest {
    private RequestInfo requestInfo;
    private TicketInput ticket;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TicketInput {
        private String category;        // INCIDENT | SERVICE_REQUEST | CHANGE | PROBLEM
        private String subCategory;
        private String priority;        // CRITICAL | HIGH | MEDIUM | LOW
        private String title;
        private String description;
        private String projectId;
        private String activityId;
        private String taskId;
        private String parentTicketUuid;
        private String assigneeUuid;
        private String assigneeName;
        private String assigneeEmail;
        private String baselineRef;     // Change tickets (FR-36.3)
        private String contractRef;
    }
}
