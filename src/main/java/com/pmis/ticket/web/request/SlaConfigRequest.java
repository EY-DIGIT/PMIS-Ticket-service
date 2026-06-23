package com.pmis.ticket.web.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SlaConfigRequest {
    private RequestInfo requestInfo;
    private SlaConfigInput slaConfig;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SlaConfigInput {
        /** null = global default; set = tenant-specific override */
        private String  tenantId;

        /** INCIDENT | SERVICE_REQUEST | CHANGE | PROBLEM */
        private String  category;

        /** CRITICAL | HIGH | MEDIUM | LOW */
        private String  priority;

        /** Hours within which a first response must be recorded */
        private Integer firstResponseHours;

        /** Hours within which the ticket must be fully resolved */
        private Integer resolutionHours;

        /** BUSINESS_HOURS | CALENDAR_HOURS */
        private String  clockType;

        /** JSON array of role codes: ["LEAD","MANAGER"] */
        private String  escalation50PctRoles;

        /** JSON array of role codes notified at 75 % consumption */
        private String  escalation75PctRoles;

        /** JSON array of role codes notified on breach */
        private String  escalationBreachRoles;

        private Boolean isActive;
    }
}
