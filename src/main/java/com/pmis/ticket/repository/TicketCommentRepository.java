package com.pmis.ticket.repository;

import com.pmis.ticket.entity.TicketCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketCommentRepository extends JpaRepository<TicketCommentEntity, String> {
    List<TicketCommentEntity> findByTicketUuidOrderByCreatedAtAsc(String ticketUuid);
}
