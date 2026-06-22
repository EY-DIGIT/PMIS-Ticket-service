package com.pmis.ticket.repository;

import com.pmis.ticket.entity.BulkOperationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BulkOperationRepository extends JpaRepository<BulkOperationEntity, String> {
    List<BulkOperationEntity> findByPerformedByOrderByCreatedAtDesc(String performedBy);
}
