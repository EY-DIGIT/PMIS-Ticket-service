package com.pmis.ticket.web.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SlaConfigResponse {
    private String uuid;
    private String category;
    private String priority;
    private Integer slaHours;
    private Integer escalationHours;
    private Boolean isActive;
}
