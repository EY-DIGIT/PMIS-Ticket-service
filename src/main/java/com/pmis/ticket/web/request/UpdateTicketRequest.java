package com.pmis.ticket.web.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UpdateTicketRequest {
    private RequestInfo requestInfo;
    private TicketUpdate ticket;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TicketUpdate {
        private String status;
        private String priority;
        private String assigneeUuid;
        private String assigneeName;
        private String assigneeEmail;
        private String comment;         // written to pmis_ticket_comment automatically
        private String title;
        private String description;
        private String baselineRef;
        private String contractRef;
    }
}
