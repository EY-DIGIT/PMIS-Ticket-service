package com.pmis.ticket.repository;

import com.pmis.ticket.entity.WorkflowConfigEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkflowConfigRepository extends JpaRepository<WorkflowConfigEntity, String> {

    Optional<WorkflowConfigEntity> findByBusinessServiceAndIsActiveTrue(String businessService);
}
