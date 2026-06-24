package com.pmis.ticket.web.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SearchTicketRequest {
    private RequestInfo requestInfo;
    private SearchCriteria criteria;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SearchCriteria {
        private String projectId;
        private String activityId;
        private String taskId;
        private String category;
        private String priority;
        private List<String> status;    // can pass multiple statuses
        private String assigneeUuid;
        private Boolean slaBreached;
        private Long fromDate;
        private Long toDate;
        private String sortBy;          // createdAt | slaDeadline | priority
        private String sortOrder;       // ASC | DESC
        private Integer offset;
        private Integer limit;
    }
}
