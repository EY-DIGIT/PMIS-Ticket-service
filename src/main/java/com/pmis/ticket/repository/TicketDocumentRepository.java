package com.pmis.ticket.repository;

import com.pmis.ticket.entity.TicketDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TicketDocumentRepository extends JpaRepository<TicketDocumentEntity, String> {

    List<TicketDocumentEntity> findByTicketUuidOrderByUploadedAtDesc(String ticketUuid);
}
