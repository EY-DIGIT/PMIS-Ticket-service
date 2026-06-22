package com.pmis.ticket.repository;

import com.pmis.ticket.entity.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TicketRepository extends JpaRepository<TicketEntity, String> {

    List<TicketEntity> findByProjectIdOrderByCreatedAtDesc(String projectId);
    List<TicketEntity> findByActivityIdOrderByCreatedAtDesc(String activityId);
    List<TicketEntity> findByTaskIdOrderByCreatedAtDesc(String taskId);
    List<TicketEntity> findByAssigneeUuidOrderByCreatedAtDesc(String assigneeUuid);
    List<TicketEntity> findByParentTicketUuidOrderByCreatedAtDesc(String parentTicketUuid);

    @Query("""
           SELECT t FROM TicketEntity t
            WHERE (:tenantId   IS NULL OR t.tenantId   = :tenantId)
              AND (:projectId  IS NULL OR t.projectId  = :projectId)
              AND (:activityId IS NULL OR t.activityId = :activityId)
              AND (:taskId     IS NULL OR t.taskId     = :taskId)
              AND (:category   IS NULL OR t.category   = :category)
              AND (:priority   IS NULL OR t.priority   = :priority)
              AND (:status     IS NULL OR t.status     = :status)
              AND (:assigneeUuid IS NULL OR t.assigneeUuid = :assigneeUuid)
              AND (:slaBreached  IS NULL OR t.slaBreached  = :slaBreached)
              AND (:fromDate   IS NULL OR t.createdAt  >= :fromDate)
              AND (:toDate     IS NULL OR t.createdAt  <= :toDate)
            ORDER BY t.createdAt DESC
           """)
    List<TicketEntity> search(
            @Param("tenantId")    String tenantId,
            @Param("projectId")   String projectId,
            @Param("activityId")  String activityId,
            @Param("taskId")      String taskId,
            @Param("category")    String category,
            @Param("priority")    String priority,
            @Param("status")      String status,
            @Param("assigneeUuid") String assigneeUuid,
            @Param("slaBreached") Boolean slaBreached,
            @Param("fromDate")    Long fromDate,
            @Param("toDate")      Long toDate);

    /** Find all open/in-progress tickets whose SLA deadline has passed. */
    @Query("""
           SELECT t FROM TicketEntity t
            WHERE t.slaBreached = false
              AND t.slaDeadline IS NOT NULL
              AND t.slaDeadline < :now
              AND t.status NOT IN ('RESOLVED','CLOSED','CANCELLED')
           """)
    List<TicketEntity> findSlaBreached(@Param("now") long now);

    @Query("SELECT COUNT(t) FROM TicketEntity t WHERE t.status NOT IN ('RESOLVED','CLOSED','CANCELLED') AND t.slaBreached = true")
    long countActiveBreached();

    Optional<TicketEntity> findByTicketNumber(String ticketNumber);

    @Query("SELECT MAX(CAST(SUBSTRING(t.ticketNumber, 10) AS int)) FROM TicketEntity t WHERE t.tenantId = :tenantId")
    Optional<Integer> findMaxSequenceForTenant(@Param("tenantId") String tenantId);
}
