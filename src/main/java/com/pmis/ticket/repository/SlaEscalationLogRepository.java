package com.pmis.ticket.repository;

import com.pmis.ticket.entity.SlaEscalationLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SlaEscalationLogRepository extends JpaRepository<SlaEscalationLogEntity, String> {

    /** Prevent duplicate escalation notifications for the same ticket + level */
    boolean existsByTicketUuidAndEscalationLevel(String ticketUuid, String escalationLevel);
}
