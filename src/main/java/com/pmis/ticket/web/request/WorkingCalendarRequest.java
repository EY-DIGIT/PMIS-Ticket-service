package com.pmis.ticket.web.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkingCalendarRequest {
    private RequestInfo requestInfo;
    private CalendarInput calendar;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CalendarInput {
        private String  name;

        /** IANA timezone e.g. "Asia/Kolkata", "UTC" */
        private String  timezone;

        /** Business day start hour (0-23) */
        private Integer workDayStart;

        /** Business day end hour (0-23) */
        private Integer workDayEnd;

        /** Comma-separated DayOfWeek names: "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY" */
        private String  workDays;

        /** JSON array of holiday dates: ["2025-01-26","2025-08-15"] */
        private String  holidays;

        private Boolean isActive;
    }
}
