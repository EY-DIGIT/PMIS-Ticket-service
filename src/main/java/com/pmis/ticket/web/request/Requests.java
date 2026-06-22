package com.pmis.ticket.web.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

// ===== REQUEST OBJECTS =====

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateTicketRequest {
    private RequestInfo requestInfo;
    private TicketInput ticket;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TicketInput {
        private String tenantId;
        private String category;        // INCIDENT | SERVICE_REQUEST | CHANGE | PROBLEM
        private String subCategory;
        private String priority;        // CRITICAL | HIGH | MEDIUM | LOW
        private String title;
        private String description;
        private String projectId;
        private String activityId;
        private String taskId;
        private String parentTicketUuid;
        private String assigneeUuid;
        private String assigneeName;
        private String assigneeEmail;
        private String baselineRef;     // Change tickets (FR-36.3)
        private String contractRef;
    }
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class UpdateTicketRequest {
    private RequestInfo requestInfo;
    private TicketUpdate ticket;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class TicketUpdate {
        private String status;
        private String priority;
        private String assigneeUuid;
        private String assigneeName;
        private String assigneeEmail;
        private String comment;         // written to pmis_ticket_comment automatically
        private String title;
        private String description;
        private String baselineRef;
        private String contractRef;
    }
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class SearchTicketRequest {
    private RequestInfo requestInfo;
    private SearchCriteria criteria;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class SearchCriteria {
        private String tenantId;
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

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class BulkOperationRequest {
    private RequestInfo requestInfo;
    private BulkOp operation;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BulkOp {
        private String type;            // STATUS_UPDATE | ASSIGNMENT | CLOSE
        private List<String> ticketUuids;
        private BulkPayload payload;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class BulkPayload {
        private String newStatus;
        private String assigneeUuid;
        private String assigneeName;
        private String assigneeEmail;
        private String comment;
    }
}

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class RequestInfo {
    private UserInfo userInfo;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class UserInfo {
        private String uuid;
        private String userName;
        private String email;
        private List<RoleInfo> roles;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class RoleInfo {
        private String code;
    }
}
