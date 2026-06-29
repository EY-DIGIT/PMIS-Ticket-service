package com.pmis.ticket.service.impl;

import com.pmis.ticket.entity.*;
import com.pmis.ticket.repository.*;
import com.pmis.ticket.service.*;
import com.pmis.ticket.web.request.*;
import com.pmis.ticket.web.response.*;
import com.pmis.ticket.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketRepository            ticketRepo;
    private final TicketCommentRepository     commentRepo;
    private final TicketAttachmentRepository  attachmentRepo;
    private final SlaConfigRepository         slaConfigRepo;
    private final BulkOperationRepository     bulkOpRepo;
    private final WorkingCalendarRepository   calendarRepo;
    private final SlaEscalationLogRepository  escalationLogRepo;
    private final SlaCalculatorService        slaCalculator;
    private final NotificationService         notificationService;
    private final DocumentService             documentService;

    // =========================================================
    // CREATE  (FR-35, FR-36, FR-37)
    // =========================================================
    @Override
    @Transactional
    public TicketResponse create(CreateTicketRequest req) {
        var input = req.getTicket();
        var user  = req.getRequestInfo().getUserInfo();
        long now  = System.currentTimeMillis();

        validate(input.getCategory(), input.getPriority(), input.getTitle());

        Optional<SlaConfigEntity> slaOpt = findSlaConfig(input.getCategory(), input.getPriority());
        WorkingCalendarEntity cal = findWorkingCalendar();

        Long firstResponseDeadline = slaOpt
                .filter(c -> c.getFirstResponseHours() != null)
                .map(c -> slaCalculator.computeDeadline(now, c.getFirstResponseHours(), c.getClockType(), cal))
                .orElse(null);

        Long resolutionDeadline = slaOpt
                .map(c -> slaCalculator.computeDeadline(now, c.getSlaHours(), c.getClockType(), cal))
                .orElse(null);

        TicketEntity ticket = TicketEntity.builder()
                .uuid(UUID.randomUUID().toString())
                .ticketNumber(nextTicketNumber())
                .ticketType(blank2null(input.getTicketType()))
                .category(input.getCategory())
                .subCategory(blank2null(input.getSubCategory()))
                .priority(input.getPriority())
                .title(input.getTitle())
                .description(blank2null(input.getDescription()))
                .status("OPEN")
                .projectId(blank2null(input.getProjectId()))
                .projectName(blank2null(input.getProjectName()))
                .activityId(blank2null(input.getActivityId()))
                .activityName(blank2null(input.getActivityName()))
                .taskId(blank2null(input.getTaskId()))
                .taskName(blank2null(input.getTaskName()))
                .parentTicketUuid(blank2null(input.getParentTicketUuid()))
                .assigneeUuid(blank2null(input.getAssigneeUuid()))
                .assigneeName(blank2null(input.getAssigneeName()))
                .assigneeEmail(blank2null(input.getAssigneeEmail()))
                .baselineRef(blank2null(input.getBaselineRef()))
                .contractRef(blank2null(input.getContractRef()))
                .reportedByUuid(user.getUuid())
                .reportedByName(user.getUserName())
                .reportedByEmail(user.getEmail())
                .firstResponseDeadline(firstResponseDeadline)
                .firstResponseBreached(false)
                .firstResponseAt(input.getAssigneeUuid() != null ? now : null)
                .slaDeadline(resolutionDeadline)
                .slaBreached(false)
                .totalPausedMs(0L)
                .slaStatus(resolutionDeadline != null ? "ON_TRACK" : "NO_SLA")
                .createdAt(now)
                .updatedAt(now)
                .build();

        ticketRepo.save(ticket);

        if (ticket.getAssigneeUuid() != null) {
            notificationService.notifyTicketCreated(ticket);
        }

        String deadlineLabel = resolutionDeadline != null ? new java.util.Date(resolutionDeadline).toString() : "N/A";
        saveSystemComment(ticket.getUuid(),
                "Ticket created · priority=" + input.getPriority()
                        + " · SLA deadline=" + deadlineLabel, user, now);

        log.info("Created ticket {} for project={} activity={}",
                ticket.getTicketNumber(), input.getProjectId(), input.getActivityId());
        return toResponse(ticket, false);
    }

    // =========================================================
    // UPDATE
    // =========================================================
    @Override
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
        if (update.getBaselineRef() != null) ticket.setBaselineRef(update.getBaselineRef());
        if (update.getContractRef() != null) ticket.setContractRef(update.getContractRef());

        if (update.getPriority() != null && !update.getPriority().equals(ticket.getPriority())) {
            ticket.setPriority(update.getPriority());
            recalculateDeadlines(ticket, now);
            saveSystemComment(ticket.getUuid(),
                    "Priority changed to " + update.getPriority() + " — SLA deadlines recalculated.", user, now);
        }

        if (update.getStatus() != null && !update.getStatus().equals(ticket.getStatus())) {
            String newStatus = update.getStatus();

            if ("PENDING".equals(newStatus) && ticket.getSlaPausedAt() == null) {
                ticket.setSlaPausedAt(now);
                ticket.setSlaStatus("PAUSED");
                saveSystemComment(ticket.getUuid(),
                        "SLA clock paused — ticket is waiting for customer response.", user, now);
            }

            if ("PENDING".equals(previousStatus) && !"PENDING".equals(newStatus)
                    && ticket.getSlaPausedAt() != null) {
                long pausedDuration = now - ticket.getSlaPausedAt();
                ticket.setTotalPausedMs(orZero(ticket.getTotalPausedMs()) + pausedDuration);
                if (ticket.getSlaDeadline() != null)
                    ticket.setSlaDeadline(ticket.getSlaDeadline() + pausedDuration);
                if (ticket.getFirstResponseDeadline() != null && ticket.getFirstResponseAt() == null)
                    ticket.setFirstResponseDeadline(ticket.getFirstResponseDeadline() + pausedDuration);
                ticket.setSlaPausedAt(null);
                saveSystemComment(ticket.getUuid(),
                        "SLA clock resumed after " + (pausedDuration / 60_000) + " min on hold. "
                                + "Deadlines extended accordingly.", user, now);
            }

            ticket.setStatus(newStatus);
            if ("RESOLVED".equals(newStatus)) ticket.setResolvedAt(now);
            if ("CLOSED".equals(newStatus))   ticket.setClosedAt(now);

            saveComment(ticket.getUuid(), "STATUS_CHANGE",
                    update.getComment(), previousStatus, newStatus,
                    null, null, user, now);
            notificationService.notifyStatusChanged(ticket, previousStatus);
            if ("RESOLVED".equals(newStatus)) {
                notificationService.notifyTicketResolved(ticket);
            }
        }

        if (update.getAssigneeUuid() != null && !update.getAssigneeUuid().equals(ticket.getAssigneeUuid())) {
            ticket.setAssigneeUuid(update.getAssigneeUuid());
            ticket.setAssigneeName(update.getAssigneeName());
            ticket.setAssigneeEmail(update.getAssigneeEmail());

            if (ticket.getFirstResponseAt() == null) {
                ticket.setFirstResponseAt(now);
                saveSystemComment(ticket.getUuid(),
                        "First response recorded — assigned to " + update.getAssigneeName(), user, now);
            }

            saveComment(ticket.getUuid(), "ASSIGNMENT",
                    update.getComment(), null, null,
                    previousAssignee, update.getAssigneeUuid(), user, now);
            notificationService.notifyTicketAssigned(ticket);
        }

        if (update.getComment() != null && update.getStatus() == null && update.getAssigneeUuid() == null) {
            saveComment(ticket.getUuid(), "COMMENT",
                    update.getComment(), null, null, null, null, user, now);
        }

        if (ticket.getSlaPausedAt() == null) {
            ticket.setSlaStatus(slaCalculator.computeSlaStatus(ticket, now));
        }

        ticket.setUpdatedAt(now);
        ticketRepo.save(ticket);
        return toResponse(ticket, false);
    }

    // =========================================================
    // GET detail
    // =========================================================
    @Override
    public TicketResponse getDetail(String uuid) {
        TicketEntity ticket = ticketRepo.findById(uuid)
                .orElseThrow(() -> new NoSuchElementException("Ticket not found: " + uuid));
        return toResponse(ticket, true);
    }

    // =========================================================
    // SEARCH
    // =========================================================
    @Override
    public SearchResponse search(SearchTicketRequest req) {
        var c = req.getCriteria();
        String statusParam = (c.getStatus() != null && !c.getStatus().isEmpty())
                ? c.getStatus().get(0) : null;

        List<TicketEntity> results = ticketRepo.search(
                c.getProjectId(), c.getActivityId(), c.getTaskId(),
                c.getCategory(), c.getPriority(), statusParam,
                c.getAssigneeUuid(), c.getSlaBreached(), c.getFromDate(), c.getToDate());

        int offset     = c.getOffset() != null ? c.getOffset() : 0;
        int limit      = c.getLimit()  != null ? c.getLimit()  : 20;
        int total      = results.size();
        int pageNumber = limit > 0 ? offset / limit : 0;
        int totalPages = limit > 0 ? (int) Math.ceil((double) total / limit) : 1;

        List<TicketEntity> page = results.stream().skip(offset).limit(limit).toList();

        return SearchResponse.builder()
                .totalCount(total)
                .page(pageNumber)
                .size(limit)
                .totalPages(totalPages)
                .tickets(page.stream().map(t -> toResponse(t, false)).toList())
                .build();
    }

    // =========================================================
    // BULK operations (FR-35.5)
    // =========================================================
    @Override
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
                ticket.setSlaStatus(slaCalculator.computeSlaStatus(ticket, now));
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

    @Override
    public BulkOperationResponse getBulkStatus(String bulkUuid) {
        BulkOperationEntity e = bulkOpRepo.findById(bulkUuid)
                .orElseThrow(() -> new NoSuchElementException("Bulk op not found: " + bulkUuid));
        return new BulkOperationResponse(e.getUuid(), e.getStatus(),
                e.getTotalCount(), e.getSuccessCount(), e.getFailedCount(), e.getCompletedAt());
    }

    // =========================================================
    // COUNTS dashboard
    // =========================================================
    @Override
    public TicketCountsResponse getCounts(String projectId, String activityId,
                                           String taskId, Long fromDate, Long toDate) {
        List<Object[]> rows = ticketRepo.countStats(projectId, activityId, taskId, fromDate, toDate);
        Object[] row = rows.isEmpty() ? new Object[7] : rows.get(0);
        long total       = toLong(row[0]);
        long resolved    = toLong(row[1]);
        long pending     = toLong(row[2]);
        long slaBreached = toLong(row[3]);
        long assigned    = toLong(row[4]);
        long notAssigned = toLong(row[5]);
        long frBreached  = toLong(row[6]);

        return TicketCountsResponse.builder()
                .total(total).created(total)
                .resolved(resolved).pending(pending)
                .slaBreached(slaBreached)
                .firstResponseBreached(frBreached)
                .assigned(assigned).notAssigned(notAssigned)
                .projectId(projectId)
                .activityId(activityId).taskId(taskId)
                .fromDate(fromDate).toDate(toDate)
                .build();
    }

    // =========================================================
    // SLA breach checker — runs every 15 minutes (FR-37.2)
    // =========================================================
    @Scheduled(fixedRate = 900_000)
    @Transactional
    public void checkSlaBreaches() {
        long now = System.currentTimeMillis();

        List<TicketEntity> newlyBreached = ticketRepo.findSlaBreached(now);
        if (!newlyBreached.isEmpty()) {
            log.info("SLA checker: {} ticket(s) newly breached resolution SLA", newlyBreached.size());
            for (TicketEntity t : newlyBreached) {
                t.setSlaBreached(true);
                t.setSlaBreachedAt(now);
                t.setSlaStatus("BREACHED");
                saveSystemComment(t.getUuid(),
                        "SLA BREACHED — resolution deadline exceeded.", null, now);
                logEscalation(t.getUuid(), "BREACH", now);
                notificationService.notifySlaEscalation(t, "BREACH");
            }
            ticketRepo.saveAll(newlyBreached);
        }

        List<TicketEntity> frBreached = ticketRepo.findFirstResponseBreached(now);
        if (!frBreached.isEmpty()) {
            log.info("SLA checker: {} ticket(s) breached first-response SLA", frBreached.size());
            for (TicketEntity t : frBreached) {
                t.setFirstResponseBreached(true);
                saveSystemComment(t.getUuid(),
                        "FIRST-RESPONSE SLA BREACHED — no response recorded within the deadline.", null, now);
                logEscalation(t.getUuid(), "FIRST_RESPONSE", now);
                notificationService.notifySlaEscalation(t, "FIRST_RESPONSE");
            }
            ticketRepo.saveAll(frBreached);
        }

        List<TicketEntity> activeTickets = ticketRepo.findActiveTicketsWithDeadline();
        List<TicketEntity> atRiskChanged = new ArrayList<>();
        for (TicketEntity t : activeTickets) {
            if (t.getSlaPausedAt() != null) continue;
            String newStatus = slaCalculator.computeSlaStatus(t, now);
            if (!newStatus.equals(t.getSlaStatus())) {
                t.setSlaStatus(newStatus);
                atRiskChanged.add(t);
                if ("AT_RISK_50".equals(newStatus)) {
                    logEscalation(t.getUuid(), "RISK_50", now);
                    notificationService.notifySlaEscalation(t, "RISK_50");
                }
                if ("AT_RISK_75".equals(newStatus)) {
                    logEscalation(t.getUuid(), "RISK_75", now);
                    notificationService.notifySlaEscalation(t, "RISK_75");
                }
            }
        }
        if (!atRiskChanged.isEmpty()) ticketRepo.saveAll(atRiskChanged);
    }

    // =========================================================
    // SLA config CRUD
    // =========================================================
    @Override
    @Transactional
    public SlaConfigResponse createSlaConfig(SlaConfigRequest req) {
        var input = req.getSlaConfig();
        long now  = System.currentTimeMillis();
        SlaConfigEntity cfg = SlaConfigEntity.builder()
                .uuid(UUID.randomUUID().toString())
                .category(input.getCategory())
                .priority(input.getPriority())
                .firstResponseHours(input.getFirstResponseHours())
                .slaHours(input.getResolutionHours())
                .clockType(input.getClockType() != null ? input.getClockType() : "BUSINESS_HOURS")
                .escalation50PctRoles(input.getEscalation50PctRoles())
                .escalation75PctRoles(input.getEscalation75PctRoles())
                .escalationBreachRoles(input.getEscalationBreachRoles())
                .isActive(input.getIsActive() != null ? input.getIsActive() : true)
                .createdAt(now).updatedAt(now)
                .build();
        return toSlaConfigResponse(slaConfigRepo.save(cfg));
    }

    @Override
    @Transactional
    public SlaConfigResponse updateSlaConfig(String uuid, SlaConfigRequest req) {
        SlaConfigEntity cfg = slaConfigRepo.findById(uuid)
                .orElseThrow(() -> new NoSuchElementException("SLA config not found: " + uuid));
        var input = req.getSlaConfig();
        long now  = System.currentTimeMillis();

        if (input.getFirstResponseHours()    != null) cfg.setFirstResponseHours(input.getFirstResponseHours());
        if (input.getResolutionHours()       != null) cfg.setSlaHours(input.getResolutionHours());
        if (input.getClockType()             != null) cfg.setClockType(input.getClockType());
        if (input.getEscalation50PctRoles()  != null) cfg.setEscalation50PctRoles(input.getEscalation50PctRoles());
        if (input.getEscalation75PctRoles()  != null) cfg.setEscalation75PctRoles(input.getEscalation75PctRoles());
        if (input.getEscalationBreachRoles() != null) cfg.setEscalationBreachRoles(input.getEscalationBreachRoles());
        if (input.getIsActive()              != null) cfg.setIsActive(input.getIsActive());
        cfg.setUpdatedAt(now);
        return toSlaConfigResponse(slaConfigRepo.save(cfg));
    }

    @Override
    public List<SlaConfigResponse> listSlaConfig() {
        return slaConfigRepo.findByIsActiveTrueOrderByCategoryAscPriorityAsc()
                .stream().map(this::toSlaConfigResponse).toList();
    }

    // =========================================================
    // Working calendar CRUD
    // =========================================================
    @Override
    @Transactional
    public WorkingCalendarResponse createCalendar(WorkingCalendarRequest req) {
        var input = req.getCalendar();
        long now  = System.currentTimeMillis();
        WorkingCalendarEntity cal = WorkingCalendarEntity.builder()
                .uuid(UUID.randomUUID().toString())
                .name(input.getName())
                .timezone(input.getTimezone() != null ? input.getTimezone() : "UTC")
                .workDayStart(input.getWorkDayStart() != null ? input.getWorkDayStart() : 9)
                .workDayEnd(input.getWorkDayEnd()     != null ? input.getWorkDayEnd()   : 18)
                .workDays(input.getWorkDays() != null ? input.getWorkDays()
                        : "MONDAY,TUESDAY,WEDNESDAY,THURSDAY,FRIDAY")
                .holidays(input.getHolidays())
                .isActive(input.getIsActive() != null ? input.getIsActive() : true)
                .createdAt(now).updatedAt(now)
                .build();
        return toCalendarResponse(calendarRepo.save(cal));
    }

    @Override
    @Transactional
    public WorkingCalendarResponse updateCalendar(String uuid, WorkingCalendarRequest req) {
        WorkingCalendarEntity cal = calendarRepo.findById(uuid)
                .orElseThrow(() -> new NoSuchElementException("Calendar not found: " + uuid));
        var input = req.getCalendar();
        long now  = System.currentTimeMillis();

        if (input.getName()         != null) cal.setName(input.getName());
        if (input.getTimezone()     != null) cal.setTimezone(input.getTimezone());
        if (input.getWorkDayStart() != null) cal.setWorkDayStart(input.getWorkDayStart());
        if (input.getWorkDayEnd()   != null) cal.setWorkDayEnd(input.getWorkDayEnd());
        if (input.getWorkDays()     != null) cal.setWorkDays(input.getWorkDays());
        if (input.getHolidays()     != null) cal.setHolidays(input.getHolidays());
        if (input.getIsActive()     != null) cal.setIsActive(input.getIsActive());
        cal.setUpdatedAt(now);
        return toCalendarResponse(calendarRepo.save(cal));
    }

    @Override
    public List<WorkingCalendarResponse> listCalendars() {
        return calendarRepo.findByIsActiveTrueOrderByNameAsc()
                .stream().map(this::toCalendarResponse).toList();
    }

    // =========================================================
    // Private helpers
    // =========================================================

    private Optional<SlaConfigEntity> findSlaConfig(String category, String priority) {
        return slaConfigRepo.findByCategoryAndPriorityAndIsActiveTrue(category, priority);
    }

    private WorkingCalendarEntity findWorkingCalendar() {
        return calendarRepo.findFirstByIsActiveTrue().orElse(null);
    }

    private void recalculateDeadlines(TicketEntity ticket, long now) {
        findSlaConfig(ticket.getCategory(), ticket.getPriority()).ifPresent(cfg -> {
            WorkingCalendarEntity cal = findWorkingCalendar();
            ticket.setSlaDeadline(slaCalculator.computeDeadline(now, cfg.getSlaHours(), cfg.getClockType(), cal));
            if (cfg.getFirstResponseHours() != null && ticket.getFirstResponseAt() == null) {
                ticket.setFirstResponseDeadline(
                        slaCalculator.computeDeadline(now, cfg.getFirstResponseHours(), cfg.getClockType(), cal));
            }
        });
    }

    private void logEscalation(String ticketUuid, String level, long now) {
        if (!escalationLogRepo.existsByTicketUuidAndEscalationLevel(ticketUuid, level)) {
            escalationLogRepo.save(SlaEscalationLogEntity.builder()
                    .uuid(UUID.randomUUID().toString())
                    .ticketUuid(ticketUuid)
                    .escalationLevel(level)
                    .escalatedAt(now)
                    .build());
            log.info("SLA escalation [{}] recorded for ticket {}", level, ticketUuid);
        }
    }

    private String nextTicketNumber() {
        int seq = ticketRepo.findMaxSequence().orElse(0) + 1;
        return "TKT-" + java.time.Year.now().getValue() + "-" + String.format("%05d", seq);
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
                if (t.getFirstResponseAt() == null) t.setFirstResponseAt(now);
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
        var validCategories = Set.of("INCIDENT", "SERVICE_REQUEST", "CHANGE", "PROBLEM");
        var validPriorities = Set.of("CRITICAL", "HIGH", "MEDIUM", "LOW");
        if (!validCategories.contains(category))
            throw new IllegalArgumentException("Invalid category: " + category);
        if (!validPriorities.contains(priority))
            throw new IllegalArgumentException("Invalid priority: " + priority);
        if (title == null || title.isBlank())
            throw new IllegalArgumentException("Title is required");
    }

    private TicketResponse toResponse(TicketEntity t, boolean includeComments) {
        long now = System.currentTimeMillis();

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

        List<TicketDocumentResponse> documents = includeComments
                ? documentService.getDocuments(t.getUuid())
                : null;

        return TicketResponse.builder()
                .uuid(t.getUuid()).ticketNumber(t.getTicketNumber())
                .ticketType(t.getTicketType())
                .category(t.getCategory()).subCategory(t.getSubCategory())
                .priority(t.getPriority()).title(t.getTitle()).description(t.getDescription())
                .status(t.getStatus())
                .projectId(t.getProjectId()).projectName(t.getProjectName())
                .activityId(t.getActivityId()).activityName(t.getActivityName())
                .taskId(t.getTaskId()).taskName(t.getTaskName())
                .parentTicketUuid(t.getParentTicketUuid()).childTickets(children)
                .assignee(t.getAssigneeUuid() != null ? new TicketResponse.UserRef(
                        t.getAssigneeUuid(), t.getAssigneeName(), t.getAssigneeEmail()) : null)
                .reporter(new TicketResponse.UserRef(
                        t.getReportedByUuid(), t.getReportedByName(), t.getReportedByEmail()))
                .slaDeadline(t.getSlaDeadline())
                .slaBreached(t.getSlaBreached())
                .slaRemainingMs(slaCalculator.slaRemainingMs(t, now))
                .slaBreachedAt(t.getSlaBreachedAt())
                .slaStatus(t.getSlaStatus())
                .firstResponseDeadline(t.getFirstResponseDeadline())
                .firstResponseBreached(t.getFirstResponseBreached())
                .firstResponseAt(t.getFirstResponseAt())
                .firstResponseRemainingMs(slaCalculator.firstResponseRemainingMs(t, now))
                .baselineRef(t.getBaselineRef()).contractRef(t.getContractRef())
                .comments(comments)
                .documents(documents)
                .createdAt(t.getCreatedAt()).updatedAt(t.getUpdatedAt())
                .resolvedAt(t.getResolvedAt()).closedAt(t.getClosedAt())
                .build();
    }

    private SlaConfigResponse toSlaConfigResponse(SlaConfigEntity e) {
        return SlaConfigResponse.builder()
                .uuid(e.getUuid())
                .category(e.getCategory())
                .priority(e.getPriority())
                .firstResponseHours(e.getFirstResponseHours())
                .slaHours(e.getSlaHours())
                .clockType(e.getClockType())
                .escalation50PctRoles(e.getEscalation50PctRoles())
                .escalation75PctRoles(e.getEscalation75PctRoles())
                .escalationBreachRoles(e.getEscalationBreachRoles())
                .isActive(e.getIsActive())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private WorkingCalendarResponse toCalendarResponse(WorkingCalendarEntity e) {
        return WorkingCalendarResponse.builder()
                .uuid(e.getUuid())
                .name(e.getName())
                .timezone(e.getTimezone())
                .workDayStart(e.getWorkDayStart())
                .workDayEnd(e.getWorkDayEnd())
                .workDays(e.getWorkDays())
                .holidays(e.getHolidays())
                .isActive(e.getIsActive())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private long toLong(Object val) {
        if (val == null) return 0L;
        if (val instanceof Long l)   return l;
        if (val instanceof Number n) return n.longValue();
        return 0L;
    }

    private long orZero(Long val) {
        return val != null ? val : 0L;
    }

    /** Returns null when the string is null or blank — prevents empty-string FK violations. */
    private String blank2null(String val) {
        return (val == null || val.isBlank()) ? null : val;
    }
}
