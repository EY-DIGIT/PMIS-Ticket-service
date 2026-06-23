package com.pmis.ticket.repository;

import com.pmis.ticket.entity.SlaConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SlaConfigRepository extends JpaRepository<SlaConfigEntity, String> {

    // ---- Tenant-aware lookup (tenant override → global fallback) ----

    /** Tenant-specific active rule */
    Optional<SlaConfigEntity> findByCategoryAndPriorityAndTenantIdAndIsActiveTrue(
            String category, String priority, String tenantId);

    /** Global default active rule (tenantId IS NULL) */
    Optional<SlaConfigEntity> findByCategoryAndPriorityAndTenantIdIsNullAndIsActiveTrue(
            String category, String priority);

    // ---- Legacy / list queries ----

    /** Kept for backward compat — prefers global defaults */
    Optional<SlaConfigEntity> findByCategoryAndPriorityAndIsActiveTrue(String category, String priority);

    List<SlaConfigEntity> findByIsActiveTrueOrderByCategoryAscPriorityAsc();

    List<SlaConfigEntity> findByTenantIdAndIsActiveTrueOrderByCategoryAscPriorityAsc(String tenantId);
}
