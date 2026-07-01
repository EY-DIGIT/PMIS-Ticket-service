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
        /** Workflow action: ASSIGN | START_PROGRESS | RESOLVE | SEND_BACK | PENDING |
         *  RESUME | RESUBMIT | CANCEL | CLOSE | REOPEN | REASSIGN */
        private String action;

        // Required for ASSIGN / REASSIGN
        private String assigneeUuid;
        private String assigneeName;
        private String assigneeEmail;

        // Required for SEND_BACK (reason shown to PMIS_ADMIN)
        private String comment;

        // Optional field updates (any action)
        private String priority;
        private String title;
        private String description;
        private String baselineRef;
        private String contractRef;
    }
}
