package com.pmis.ticket.repository;

import com.pmis.ticket.entity.WorkingCalendarEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkingCalendarRepository extends JpaRepository<WorkingCalendarEntity, String> {

    Optional<WorkingCalendarEntity> findFirstByIsActiveTrue();

    List<WorkingCalendarEntity> findByIsActiveTrueOrderByNameAsc();
}
