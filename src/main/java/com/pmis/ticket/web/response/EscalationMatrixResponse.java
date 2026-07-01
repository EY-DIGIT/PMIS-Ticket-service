package com.pmis.ticket.web.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EscalationMatrixResponse {
    private String       uuid;
    private String       priority;
    private String       level;
    private Integer      triggerHours;
    private List<String> emails;
    private Boolean      isActive;
    private Long         updatedAt;
}
