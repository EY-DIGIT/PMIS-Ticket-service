package com.pmis.ticket.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pmis_workflow_config")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class WorkflowConfigEntity {

    @Id
    @Column(name = "business_service", nullable = false, length = 64)
    private String businessService;     // e.g. PMIS-TICKET

    @Column(name = "workflow_json", nullable = false, columnDefinition = "text")
    private String workflowJson;        // full JSON stored as text

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    private Long createdAt;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    @Column(name = "updated_by", length = 128)
    private String updatedBy;
}
