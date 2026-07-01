package com.pmis.ticket.web.request;

import lombok.*;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class EscalationMatrixRequest {
    private Integer     triggerHours;
    private List<String> emails;
    private Boolean     isActive;
}
