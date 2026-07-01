package com.pmis.ticket.repository;

import com.pmis.ticket.entity.EscalationMatrixEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EscalationMatrixRepository extends JpaRepository<EscalationMatrixEntity, String> {

    List<EscalationMatrixEntity> findByIsActiveTrueOrderByPriorityAscLevelAsc();

    List<EscalationMatrixEntity> findByPriorityAndIsActiveTrueOrderByLevelAsc(String priority);

    Optional<EscalationMatrixEntity> findByPriorityAndLevel(String priority, String level);
}
