package com.pmis.ticket.repository;

import com.pmis.ticket.entity.TicketEscalationLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketEscalationLogRepository extends JpaRepository<TicketEscalationLogEntity, String> {

    List<TicketEscalationLogEntity> findByTicketUuidOrderByTriggeredAtAsc(String ticketUuid);

    Optional<TicketEscalationLogEntity> findByTicketUuidAndLevel(String ticketUuid, String level);

    boolean existsByTicketUuidAndLevel(String ticketUuid, String level);
}
