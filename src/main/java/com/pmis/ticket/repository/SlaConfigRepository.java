package com.pmis.ticket.repository;

import com.pmis.ticket.entity.SlaConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SlaConfigRepository extends JpaRepository<SlaConfigEntity, String> {
    Optional<SlaConfigEntity> findByCategoryAndPriorityAndIsActiveTrue(String category, String priority);
    List<SlaConfigEntity> findByIsActiveTrueOrderByCategoryAscPriorityAsc();
}
