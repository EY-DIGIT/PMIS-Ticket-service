package com.pmis.ticket.web.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TicketResponse {
    private String uuid;
    private String ticketNumber;
    private String tenantId;
    private String category;
    private String subCategory;
    private String priority;
    private String title;
    private String description;
    private String status;

    // linkage
    private String projectId;
    private String activityId;
    private String taskId;

    // parent-child
    private String parentTicketUuid;
    private List<TicketSummary> childTickets;

    // assignment
    private UserRef assignee;
    private UserRef reporter;

    // SLA — resolution
    private Long    slaDeadline;
    private Boolean slaBreached;
    private Long    slaRemainingMs;       // computed: effective deadline - now (null if breached)
    private Long    slaBreachedAt;
    private String  slaStatus;            // ON_TRACK | AT_RISK_50 | AT_RISK_75 | BREACHED | PAUSED | NO_SLA

    // SLA — first response
    private Long    firstResponseDeadline;
    private Boolean firstResponseBreached;
    private Long    firstResponseAt;      // epoch ms when first response was recorded
    private Long    firstResponseRemainingMs; // null if already responded or deadline passed

    // Change extras
    private String baselineRef;
    private String contractRef;

    // comments + attachments (only on detail GET)
    private List<CommentResponse> comments;

    // audit
    private Long createdAt;
    private Long updatedAt;
    private Long resolvedAt;
    private Long closedAt;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class UserRef {
        private String uuid;
        private String name;
        private String email;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class TicketSummary {
        private String uuid;
        private String ticketNumber;
        private String title;
        private String status;
        private String priority;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class CommentResponse {
        private String uuid;
        private String commentType;   // COMMENT | STATUS_CHANGE | ASSIGNMENT | SYSTEM
        private String body;
        private String previousStatus;
        private String newStatus;
        private String previousAssignee;
        private String newAssignee;
        private UserRef author;
        private Long createdAt;
        private List<AttachmentResponse> attachments;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class AttachmentResponse {
        private String uuid;
        private String fileName;
        private String mimeType;
        private Long   sizeBytes;
        private String fileUrl;
        private Long   uploadedAt;
    }
}
