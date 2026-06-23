package com.pmis.ticket.web.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkingCalendarResponse {
    private String  uuid;
    private String  tenantId;        // null = global default
    private String  name;
    private String  timezone;
    private Integer workDayStart;    // hour 0-23
    private Integer workDayEnd;      // hour 0-23
    private String  workDays;        // "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY"
    private String  holidays;        // JSON array of "YYYY-MM-DD" strings
    private Boolean isActive;
    private Long    createdAt;
    private Long    updatedAt;
}
