package com.pmis.ticket.web.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SlaConfigResponse {
    private String  uuid;
    private String  category;
    private String  priority;
    private Integer firstResponseHours;
    private Integer slaHours;             // resolution hours
    private String  clockType;            // BUSINESS_HOURS | CALENDAR_HOURS
    private String  escalation50PctRoles; // JSON array
    private String  escalation75PctRoles; // JSON array
    private String  escalationBreachRoles;// JSON array
    private Boolean isActive;
    private Long    createdAt;
    private Long    updatedAt;
}
