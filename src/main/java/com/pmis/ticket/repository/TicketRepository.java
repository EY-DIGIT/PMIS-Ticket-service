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
            WHERE (:projectId  IS NULL OR t.projectId  = :projectId)
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

    /** Tickets whose resolution SLA has just been breached (not yet flagged). */
    @Query("""
           SELECT t FROM TicketEntity t
            WHERE t.slaBreached = false
              AND t.slaDeadline IS NOT NULL
              AND t.slaDeadline < :now
              AND t.status NOT IN ('RESOLVED','CLOSED','CANCELLED')
           """)
    List<TicketEntity> findSlaBreached(@Param("now") long now);

    /** Tickets whose first-response deadline has passed with no response recorded. */
    @Query("""
           SELECT t FROM TicketEntity t
            WHERE t.firstResponseBreached = false
              AND t.firstResponseAt IS NULL
              AND t.firstResponseDeadline IS NOT NULL
              AND t.firstResponseDeadline < :now
              AND t.status NOT IN ('RESOLVED','CLOSED','CANCELLED')
           """)
    List<TicketEntity> findFirstResponseBreached(@Param("now") long now);

    /** Active tickets that have a resolution deadline — used for AT_RISK percentage updates. */
    @Query("""
           SELECT t FROM TicketEntity t
            WHERE t.slaDeadline IS NOT NULL
              AND t.slaBreached = false
              AND t.slaPausedAt IS NULL
              AND t.status NOT IN ('RESOLVED','CLOSED','CANCELLED')
           """)
    List<TicketEntity> findActiveTicketsWithDeadline();

    @Query("SELECT COUNT(t) FROM TicketEntity t WHERE t.status NOT IN ('RESOLVED','CLOSED','CANCELLED') AND t.slaBreached = true")
    long countActiveBreached();

    Optional<TicketEntity> findByTicketNumber(String ticketNumber);

    @Query("SELECT MAX(CAST(SUBSTRING(t.ticketNumber, 10) AS int)) FROM TicketEntity t")
    Optional<Integer> findMaxSequence();

    /**
     * Single-pass aggregate: returns Object[7]
     *   [0] total
     *   [1] resolved             (RESOLVED | CLOSED)
     *   [2] pending              (not RESOLVED | CLOSED | CANCELLED)
     *   [3] slaBreached
     *   [4] assigned             (assigneeUuid IS NOT NULL)
     *   [5] notAssigned
     *   [6] firstResponseBreached
     */
    @Query("""
           SELECT COUNT(t),
                  SUM(CASE WHEN t.status IN ('RESOLVED','CLOSED') THEN 1 ELSE 0 END),
                  SUM(CASE WHEN t.status NOT IN ('RESOLVED','CLOSED','CANCELLED') THEN 1 ELSE 0 END),
                  SUM(CASE WHEN t.slaBreached = true THEN 1 ELSE 0 END),
                  SUM(CASE WHEN t.assigneeUuid IS NOT NULL THEN 1 ELSE 0 END),
                  SUM(CASE WHEN t.assigneeUuid IS NULL THEN 1 ELSE 0 END),
                  SUM(CASE WHEN t.firstResponseBreached = true THEN 1 ELSE 0 END)
           FROM TicketEntity t
           WHERE (:projectId  IS NULL OR t.projectId  = :projectId)
             AND (:activityId IS NULL OR t.activityId = :activityId)
             AND (:taskId     IS NULL OR t.taskId     = :taskId)
             AND (:fromDate   IS NULL OR t.createdAt  >= :fromDate)
             AND (:toDate     IS NULL OR t.createdAt  <= :toDate)
           """)
    Object[] countStats(
            @Param("projectId")   String projectId,
            @Param("activityId")  String activityId,
            @Param("taskId")      String taskId,
            @Param("fromDate")    Long fromDate,
            @Param("toDate")      Long toDate);
}
