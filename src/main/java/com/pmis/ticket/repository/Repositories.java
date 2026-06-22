package com.pmis.ticket.repository;

import com.pmis.ticket.entity.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketCommentRepository extends JpaRepository<TicketCommentEntity, String> {
    List<TicketCommentEntity> findByTicketUuidOrderByCreatedAtAsc(String ticketUuid);
}

@Repository
interface TicketAttachmentRepository extends JpaRepository<TicketAttachmentEntity, String> {
    List<TicketAttachmentEntity> findByTicketUuid(String ticketUuid);
    List<TicketAttachmentEntity> findByCommentUuid(String commentUuid);
}

@Repository
interface SlaConfigRepository extends JpaRepository<SlaConfigEntity, String> {
    Optional<SlaConfigEntity> findByCategoryAndPriorityAndIsActiveTrue(String category, String priority);
    List<SlaConfigEntity> findByIsActiveTrueOrderByCategoryAscPriorityAsc();
}

@Repository
interface BulkOperationRepository extends JpaRepository<BulkOperationEntity, String> {
    List<BulkOperationEntity> findByPerformedByOrderByCreatedAtDesc(String performedBy);
}
