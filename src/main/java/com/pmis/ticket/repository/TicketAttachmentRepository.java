package com.pmis.ticket.repository;

import com.pmis.ticket.entity.TicketAttachmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketAttachmentRepository extends JpaRepository<TicketAttachmentEntity, String> {
    List<TicketAttachmentEntity> findByTicketUuid(String ticketUuid);
    List<TicketAttachmentEntity> findByCommentUuid(String commentUuid);
}
