package com.pmis.ticket.web.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TicketCountsResponse {

    // ── counts ────────────────────────────────────────────────────────────────
    private long total;                  // all tickets matching the filter
    private long created;                // alias for total (same value, for dashboard clarity)
    private long resolved;               // status IN (RESOLVED, CLOSED)
    private long pending;                // status NOT IN (RESOLVED, CLOSED, CANCELLED)
    private long slaBreached;            // slaBreached = true
    private long firstResponseBreached;  // firstResponseBreached = true
    private long assigned;               // assigneeUuid IS NOT NULL
    private long notAssigned;            // assigneeUuid IS NULL

    // ── filters applied (echoed back, null when not supplied) ─────────────────
    private String tenantId;
    private String projectId;
    private String activityId;
    private String taskId;
    private Long   fromDate;
    private Long   toDate;
}
