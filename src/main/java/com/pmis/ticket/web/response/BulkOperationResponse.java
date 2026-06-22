package com.pmis.ticket.web.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BulkOperationResponse {
    private String bulkOperationUuid;
    private String status;
    private int totalCount;
    private int successCount;
    private int failedCount;
    private Long completedAt;
}
