package com.pmis.ticket.service;

import com.pmis.ticket.entity.*;
import com.pmis.ticket.repository.*;
import com.pmis.ticket.web.request.*;
import com.pmis.ticket.web.response.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepo;
    private final TicketCommentRepository commentRepo;
    private final TicketAttachmentRepository attachmentRepo;
    private final SlaConfigRepository slaConfigRepo;
    private final BulkOperationRepository bulkOpRepo;

    // =========================================================
    // CREATE  (FR-35, FR-36, FR-37)
    // =========================================================
    @Transactional
    public TicketResponse create(CreateTicketRequest req) {
        var input = req.getTicket();
        var user  = req.getRequestInfo().getUserInfo();
        long now  = System.currentTimeMillis();

        validate(input.getCategory(), input.getPriority(), input.getTitle());

        // SLA deadline
        Long deadline = computeDeadline(input.getCategory(), input.getPriority(), now);

        TicketEntity ticket = TicketEntity.builder()
                .uuid(UUID.randomUUID().toString())
                .ticketNumber(nextTicketNumber(input.getTenantId()))
                .tenantId(input.getTenantId())
                .category(input.getCategory())
                .subCategory(input.getSubCategory())
                .priority(input.getPriority())
                .title(input.getTitle())
                .description(input.getDescription())
                .status("OPEN")
                .projectId(input.getProjectId())
                .activityId(input.getActivityId())
                .taskId(input.getTaskId())
                .parentTicketUuid(input.getParentTicketUuid())
                .assigneeUuid(input.getAssigneeUuid())
                .assigneeName(input.getAssigneeName())
                .assigneeEmail(input.getAssigneeEmail())
                .baselineRef(input.getBaselineRef())
                .contractRef(input.getContractRef())
                .reportedByUuid(user.getUuid())
                .reportedByName(user.getUserName())
                .reportedByEmail(user.getEmail())
                .slaDeadline(deadline)
                .slaBreached(false)
                .createdAt(now)
                .updatedAt(now)
                .build();

        ticketRepo.save(ticket);

        // system comment: ticket created
        saveSystemComment(ticket.getUuid(), "Ticket created with priority " + input.getPriority()
                + " — SLA deadline: " + (deadline != null ? new java.util.Date(deadline) : "N/A"),
                user, now);

        log.info("Created ticket {} for project={} activity={}",
                ticket.getTicketNumber(), input.getProjectId(), input.getActivityId());
        return toResponse(ticket, false);
    }

    // =========================================================
    // UPDATE (status / assignee / fields)
    // =========================================================
    @Transactional
    public TicketResponse update(String uuid, UpdateTicketRequest req) {
        TicketEntity ticket = ticketRepo.findById(uuid)
                .orElseThrow(() -> new NoSuchElementException("Ticket not found: " + uuid));
        var update = req.getTicket();
        var user   = req.getRequestInfo().getUserInfo();
        long now   = System.currentTimeMillis();

        String previousStatus   = ticket.getStatus();
        String previousAssignee = ticket.getAssigneeUuid();

        if (update.getTitle()       != null) ticket.setTitle(update.getTitle());
        if (update.getDescription() != null) ticket.setDescription(update.getDescription());
        if (update.getPriority()    != null) ticket.setPriority(update.getPriority());
        if (update.getBaselineRef() != null) ticket.setBaselineRef(update.getBaselineRef());
        if (update.getContractRef() != null) ticket.setContractRef(update.getContractRef());

        // status change
        if (update.getStatus() != null && !update.getStatus().equals(ticket.getStatus())) {
            ticket.setStatus(update.getStatus());
            if ("RESOLVED".equals(update.getStatus())) ticket.setResolvedAt(now);
            if ("CLOSED".equals(update.getStatus()))   ticket.setClosedAt(now);

            saveComment(ticket.getUuid(), "STATUS_CHANGE",
                    update.getComment(), previousStatus, update.getStatus(),
                    null, null, user, now);
        }

        // assignment change
        if (update.getAssigneeUuid() != null && !update.getAssigneeUuid().equals(ticket.getAssigneeUuid())) {
            ticket.setAssigneeUuid(update.getAssigneeUuid());
            ticket.setAssigneeName(update.getAssigneeName());
            ticket.setAssigneeEmail(update.getAssigneeEmail());

            saveComment(ticket.getUuid(), "ASSIGNMENT",
                    update.getComment(), null, null,
                    previousAssignee, update.getAssigneeUuid(), user, now);
        }

        // plain comment
        if (update.getComment() != null && update.getStatus() == null && update.getAssigneeUuid() == null) {
            saveComment(ticket.getUuid(), "COMMENT",
                    update.getComment(), null, null, null, null, user, now);
        }

        ticket.setUpdatedAt(now);
        ticketRepo.save(ticket);
        return toResponse(ticket, false);
    }

    // =========================================================
    // GET detail
    // =========================================================
    public TicketResponse getDetail(String uuid) {
        TicketEntity ticket = ticketRepo.findById(uuid)
                .orElseThrow(() -> new NoSuchElementException("Ticket not found: " + uuid));
        return toResponse(ticket, true);
    }

    // =========================================================
    // SEARCH  (FR-35.5 bulk query)
    // =========================================================
    public SearchResponse search(SearchTicketRequest req) {
        var c = req.getCriteria();
        // Join multi-status into first value for simplicity;
        // for full multi-status extend the JPQL with IN clause.
        String statusParam = (c.getStatus() != null && !c.getStatus().isEmpty())
                ? c.getStatus().get(0) : null;

        List<TicketEntity> results = ticketRepo.search(
                c.getTenantId(), c.getProjectId(), c.getActivityId(), c.getTaskId(),
                c.getCategory(), c.getPriority(), statusParam,
                c.getAssigneeUuid(), c.getSlaBreached(), c.getFromDate(), c.getToDate());

        // pagination
        int offset = c.getOffset() != null ? c.getOffset() : 0;
        int limit  = c.getLimit()  != null ? c.getLimit()  : 20;
        List<TicketEntity> page = results.stream().skip(offset).limit(limit).toList();

        return new SearchResponse(results.size(),
                page.stream().map(t -> toResponse(t, false)).toList());
    }

    // =========================================================
    // BULK operations (FR-35.5)
    // =========================================================
    @Transactional
    public BulkOperationResponse bulk(BulkOperationRequest req) {
        var op   = req.getOperation();
        var user = req.getRequestInfo().getUserInfo();
        long now = System.currentTimeMillis();

        BulkOperationEntity entity = BulkOperationEntity.builder()
                .uuid(UUID.randomUUID().toString())
                .operationType(op.getType())
                .ticketUuids(op.getTicketUuids().toArray(new String[0]))
                .totalCount(op.getTicketUuids().size())
                .successCount(0).failedCount(0)
                .status("PROCESSING")
                .performedBy(user.getUuid())
                .createdAt(now)
                .build();

        bulkOpRepo.save(entity);

        int success = 0, failed = 0;
        for (String ticketUuid : op.getTicketUuids()) {
            try {
                TicketEntity ticket = ticketRepo.findById(ticketUuid).orElseThrow();
                applyBulkOp(ticket, op, user, now);
                ticketRepo.save(ticket);
                success++;
            } catch (Exception ex) {
                log.warn("Bulk op failed for ticket {}: {}", ticketUuid, ex.getMessage());
                failed++;
            }
        }

        entity.setSuccessCount(success);
        entity.setFailedCount(failed);
        entity.setStatus(failed == 0 ? "DONE" : "FAILED");
        entity.setCompletedAt(System.currentTimeMillis());
        bulkOpRepo.save(entity);

        return new BulkOperationResponse(entity.getUuid(), entity.getStatus(),
                entity.getTotalCount(), success, failed, entity.getCompletedAt());
    }

    public BulkOperationResponse getBulkStatus(String bulkUuid) {
        BulkOperationEntity e = bulkOpRepo.findById(bulkUuid)
                .orElseThrow(() -> new NoSuchElementException("Bulk op not found: " + bulkUuid));
        return new BulkOperationResponse(e.getUuid(), e.getStatus(),
                e.getTotalCount(), e.getSuccessCount(), e.getFailedCount(), e.getCompletedAt());
    }

    // =========================================================
    // SLA breach checker (FR-37.2) - runs every 15 minutes
    // =========================================================
    @Scheduled(fixedRate = 900_000)
    @Transactional
    public void checkSlaBreaches() {
        long now     = System.currentTimeMillis();
        var breached = ticketRepo.findSlaBreached(now);
        if (breached.isEmpty()) return;

        log.info("SLA checker: {} ticket(s) breached SLA", breached.size());
        for (TicketEntity t : breached) {
            t.setSlaBreached(true);
            t.setSlaBreachedAt(now);
            saveSystemComment(t.getUuid(),
                    "SLA BREACHED — ticket has exceeded the resolution deadline.", null, now);
        }
        ticketRepo.saveAll(breached);
    }

    // =========================================================
    // SLA config
    // =========================================================
    public List<SlaConfigResponse> listSlaConfig() {
        return slaConfigRepo.findByIsActiveTrueOrderByCategoryAscPriorityAsc().stream()
                .map(this::toSlaConfigResponse).toList();
    }

    // =========================================================
    // Helpers
    // =========================================================
    private String nextTicketNumber(String tenantId) {
        int seq = ticketRepo.findMaxSequenceForTenant(tenantId).orElse(0) + 1;
        return "TKT-" + java.time.Year.now().getValue() + "-" + String.format("%05d", seq);
    }

    private Long computeDeadline(String category, String priority, long now) {
        return slaConfigRepo.findByCategoryAndPriorityAndIsActiveTrue(category, priority)
                .map(cfg -> now + ((long) cfg.getSlaHours() * 3_600_000L))
                .orElse(null);
    }

    private void applyBulkOp(TicketEntity t,
                              BulkOperationRequest.BulkOp op,
                              RequestInfo.UserInfo user, long now) {
        var p = op.getPayload();
        switch (op.getType()) {
            case "STATUS_UPDATE" -> {
                saveComment(t.getUuid(), "STATUS_CHANGE", p.getComment(),
                        t.getStatus(), p.getNewStatus(), null, null, user, now);
                t.setStatus(p.getNewStatus());
                if ("RESOLVED".equals(p.getNewStatus())) t.setResolvedAt(now);
                if ("CLOSED".equals(p.getNewStatus()))   t.setClosedAt(now);
            }
            case "ASSIGNMENT" -> {
                saveComment(t.getUuid(), "ASSIGNMENT", p.getComment(),
                        null, null, t.getAssigneeUuid(), p.getAssigneeUuid(), user, now);
                t.setAssigneeUuid(p.getAssigneeUuid());
                t.setAssigneeName(p.getAssigneeName());
                t.setAssigneeEmail(p.getAssigneeEmail());
            }
            case "CLOSE" -> {
                saveComment(t.getUuid(), "STATUS_CHANGE", p.getComment(),
                        t.getStatus(), "CLOSED", null, null, user, now);
                t.setStatus("CLOSED");
                t.setClosedAt(now);
            }
        }
        t.setUpdatedAt(now);
    }

    private void saveComment(String ticketUuid, String type, String body,
                              String prevStatus, String newStatus,
                              String prevAssignee, String newAssignee,
                              RequestInfo.UserInfo user, long now) {
        commentRepo.save(TicketCommentEntity.builder()
                .uuid(UUID.randomUUID().toString())
                .ticketUuid(ticketUuid)
                .commentType(type)
                .body(body)
                .previousStatus(prevStatus)
                .newStatus(newStatus)
                .previousAssignee(prevAssignee)
                .newAssignee(newAssignee)
                .authorUuid(user != null ? user.getUuid() : "SYSTEM")
                .authorName(user != null ? user.getUserName() : "System")
                .authorEmail(user != null ? user.getEmail() : null)
                .createdAt(now)
                .build());
    }

    private void saveSystemComment(String ticketUuid, String body,
                                    RequestInfo.UserInfo user, long now) {
        saveComment(ticketUuid, "SYSTEM", body, null, null, null, null, user, now);
    }

    private void validate(String category, String priority, String title) {
        var validCategories = Set.of("INCIDENT","SERVICE_REQUEST","CHANGE","PROBLEM");
        var validPriorities = Set.of("CRITICAL","HIGH","MEDIUM","LOW");
        if (!validCategories.contains(category))
            throw new IllegalArgumentException("Invalid category: " + category);
        if (!validPriorities.contains(priority))
            throw new IllegalArgumentException("Invalid priority: " + priority);
        if (title == null || title.isBlank())
            throw new IllegalArgumentException("Title is required");
    }

    private TicketResponse toResponse(TicketEntity t, boolean includeComments) {
        long now = System.currentTimeMillis();
        Long remaining = (t.getSlaDeadline() != null && !Boolean.TRUE.equals(t.getSlaBreached()))
                ? Math.max(0, t.getSlaDeadline() - now) : null;

        List<TicketResponse.CommentResponse> comments = null;
        if (includeComments) {
            var rawComments = commentRepo.findByTicketUuidOrderByCreatedAtAsc(t.getUuid());
            comments = rawComments.stream().map(c -> {
                var attachments = attachmentRepo.findByCommentUuid(c.getUuid()).stream()
                        .map(a -> TicketResponse.AttachmentResponse.builder()
                                .uuid(a.getUuid()).fileName(a.getFileName())
                                .mimeType(a.getMimeType()).sizeBytes(a.getSizeBytes())
                                .fileUrl(a.getFileUrl()).uploadedAt(a.getUploadedAt())
                                .build()).toList();
                return TicketResponse.CommentResponse.builder()
                        .uuid(c.getUuid()).commentType(c.getCommentType()).body(c.getBody())
                        .previousStatus(c.getPreviousStatus()).newStatus(c.getNewStatus())
                        .previousAssignee(c.getPreviousAssignee()).newAssignee(c.getNewAssignee())
                        .author(new TicketResponse.UserRef(c.getAuthorUuid(), c.getAuthorName(), c.getAuthorEmail()))
                        .createdAt(c.getCreatedAt()).attachments(attachments)
                        .build();
            }).toList();
        }

        List<TicketResponse.TicketSummary> children = ticketRepo
                .findByParentTicketUuidOrderByCreatedAtDesc(t.getUuid()).stream()
                .map(c -> new TicketResponse.TicketSummary(
                        c.getUuid(), c.getTicketNumber(), c.getTitle(), c.getStatus(), c.getPriority()))
                .toList();

        return TicketResponse.builder()
                .uuid(t.getUuid()).ticketNumber(t.getTicketNumber()).tenantId(t.getTenantId())
                .category(t.getCategory()).subCategory(t.getSubCategory())
                .priority(t.getPriority()).title(t.getTitle()).description(t.getDescription())
                .status(t.getStatus())
                .projectId(t.getProjectId()).activityId(t.getActivityId()).taskId(t.getTaskId())
                .parentTicketUuid(t.getParentTicketUuid()).childTickets(children)
                .assignee(t.getAssigneeUuid() != null ? new TicketResponse.UserRef(
                        t.getAssigneeUuid(), t.getAssigneeName(), t.getAssigneeEmail()) : null)
                .reporter(new TicketResponse.UserRef(
                        t.getReportedByUuid(), t.getReportedByName(), t.getReportedByEmail()))
                .slaDeadline(t.getSlaDeadline()).slaBreached(t.getSlaBreached())
                .slaRemainingMs(remaining).slaBreachedAt(t.getSlaBreachedAt())
                .baselineRef(t.getBaselineRef()).contractRef(t.getContractRef())
                .comments(comments)
                .createdAt(t.getCreatedAt()).updatedAt(t.getUpdatedAt())
                .resolvedAt(t.getResolvedAt()).closedAt(t.getClosedAt())
                .build();
    }

    private SlaConfigResponse toSlaConfigResponse(SlaConfigEntity e) {
        return new SlaConfigResponse(e.getUuid(), e.getCategory(), e.getPriority(),
                e.getSlaHours(), e.getEscalationHours(), e.getIsActive());
    }
}
