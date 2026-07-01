package com.pmis.ticket.service.impl;

import com.pmis.ticket.config.NotificationProperties;
import com.pmis.ticket.entity.EscalationMatrixEntity;
import com.pmis.ticket.entity.TicketEntity;
import com.pmis.ticket.entity.TicketEscalationLogEntity;
import com.pmis.ticket.repository.EscalationMatrixRepository;
import com.pmis.ticket.repository.TicketEscalationLogRepository;
import com.pmis.ticket.repository.TicketRepository;
import com.pmis.ticket.service.EscalationService;
import com.pmis.ticket.web.request.EscalationMatrixRequest;
import com.pmis.ticket.web.response.EscalationLogResponse;
import com.pmis.ticket.web.response.EscalationMatrixResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class EscalationServiceImpl implements EscalationService {

    private final EscalationMatrixRepository    matrixRepo;
    private final TicketEscalationLogRepository logRepo;
    private final TicketRepository              ticketRepo;
    private final NotificationProperties        props;
    private final RestClient                    notificationRestClient;

    // =========================================================
    // Scheduler — runs every 15 minutes
    // =========================================================

    @Scheduled(fixedRate = 900_000)
    @Transactional
    public void checkEscalations() {
        long now = System.currentTimeMillis();
        List<String> activeStatuses = List.of("OPEN", "IN_PROGRESS", "PENDING");

        List<TicketEntity> tickets = ticketRepo.findAll().stream()
                .filter(t -> activeStatuses.contains(t.getStatus()))
                .toList();

        if (tickets.isEmpty()) return;

        List<EscalationMatrixEntity> allRules = matrixRepo.findByIsActiveTrueOrderByPriorityAscLevelAsc();
        if (allRules.isEmpty()) return;

        for (TicketEntity ticket : tickets) {
            long hoursElapsed = (now - ticket.getCreatedAt()) / 3_600_000L;
            String priority   = normalizePriority(ticket.getPriority());
            if (priority == null) continue;

            allRules.stream()
                    .filter(r -> r.getPriority().equals(priority))
                    .forEach(rule -> {
                        if (hoursElapsed >= rule.getTriggerHours()
                                && !logRepo.existsByTicketUuidAndLevel(ticket.getUuid(), rule.getLevel())) {
                            fireEscalation(ticket, rule, now);
                        }
                    });
        }
    }

    // =========================================================
    // Public API
    // =========================================================

    @Override
    public List<EscalationMatrixResponse> listMatrix() {
        return matrixRepo.findByIsActiveTrueOrderByPriorityAscLevelAsc()
                .stream().map(this::toMatrixResponse).toList();
    }

    @Override
    @Transactional
    public EscalationMatrixResponse updateMatrix(String uuid, EscalationMatrixRequest req) {
        EscalationMatrixEntity entity = matrixRepo.findById(uuid)
                .orElseThrow(() -> new NoSuchElementException("Escalation matrix entry not found: " + uuid));

        if (req.getTriggerHours() != null) entity.setTriggerHours(req.getTriggerHours());
        if (req.getEmails()       != null) entity.setEmails(req.getEmails());
        if (req.getIsActive()     != null) entity.setIsActive(req.getIsActive());
        entity.setUpdatedAt(System.currentTimeMillis());

        return toMatrixResponse(matrixRepo.save(entity));
    }

    @Override
    public List<EscalationLogResponse> getLogsForTicket(String ticketUuid) {
        return logRepo.findByTicketUuidOrderByTriggeredAtAsc(ticketUuid)
                .stream().map(this::toLogResponse).toList();
    }

    // =========================================================
    // Internal
    // =========================================================

    private void fireEscalation(TicketEntity ticket, EscalationMatrixEntity rule, long now) {
        List<String> emails = rule.getEmails();
        if (emails == null || emails.isEmpty()) return;

        // Save log first (idempotency — prevent duplicate sends on retry)
        TicketEscalationLogEntity logEntry = TicketEscalationLogEntity.builder()
                .uuid(UUID.randomUUID().toString())
                .ticketUuid(ticket.getUuid())
                .ticketNumber(ticket.getTicketNumber())
                .priority(rule.getPriority())
                .level(rule.getLevel())
                .triggerHours(rule.getTriggerHours())
                .emailsSent(emails)
                .triggeredAt(now)
                .build();
        logRepo.save(logEntry);

        // Send notification async
        if (props.isEnabled()) {
            sendEscalationEmail(ticket, rule, emails, now);
        }

        log.info("Escalation fired: ticket={} priority={} level={} emails={}",
                ticket.getTicketNumber(), rule.getPriority(), rule.getLevel(), emails);
    }

    private void sendEscalationEmail(TicketEntity ticket, EscalationMatrixEntity rule,
                                      List<String> to, long now) {
        try {
            String subject = String.format("PMIS — Escalation %s | %s | %s",
                    rule.getLevel(), ticket.getPriority(), ticket.getTicketNumber());

            String body = buildEscalationBody(ticket, rule, now);

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("to",      to);
            payload.put("cc",      List.of());
            payload.put("subject", subject);
            payload.put("body",    body);

            notificationRestClient.post().body(payload).retrieve().toBodilessEntity();
            log.info("Escalation email sent [{}] -> {}", rule.getLevel(), to);
        } catch (Exception ex) {
            log.error("Escalation email failed [{} {}]: {}", rule.getPriority(), rule.getLevel(), ex.getMessage());
        }
    }

    private String buildEscalationBody(TicketEntity t, EscalationMatrixEntity rule, long now) {
        long hoursElapsed = (now - t.getCreatedAt()) / 3_600_000L;
        return """
                ESCALATION ALERT — %s

                This ticket has not been resolved within the expected timeframe and has been escalated to %s.

                  Ticket No      : %s
                  Title          : %s
                  Priority       : %s
                  Status         : %s
                  Category       : %s
                  Ticket Type    : %s
                  Project        : %s
                  Activity       : %s
                  Task           : %s
                  Raised By      : %s
                  Assigned To    : %s
                  Created At     : %s
                  Hours Elapsed  : %d hrs
                  SLA Deadline   : %s
                  SLA Status     : %s

                Please take immediate action.

                — PMIS Ticket System
                """.formatted(
                rule.getLevel(),
                rule.getLevel(),
                t.getTicketNumber(),
                t.getTitle(),
                t.getPriority(),
                t.getStatus(),
                t.getCategory(),
                orDash(t.getTicketType()),
                orDash(t.getProjectName()),
                orDash(t.getActivityName()),
                orDash(t.getTaskName()),
                orDash(t.getReportedByName()),
                orDash(t.getAssigneeName()),
                t.getCreatedAt() != null ? new java.util.Date(t.getCreatedAt()) : "N/A",
                hoursElapsed,
                t.getSlaDeadline()  != null ? new java.util.Date(t.getSlaDeadline()) : "N/A",
                orDash(t.getSlaStatus())
        );
    }

    /** Maps ticket priority label to matrix key. */
    private String normalizePriority(String priority) {
        if (priority == null) return null;
        return switch (priority.toUpperCase()) {
            case "CRITICAL", "P1" -> "P1";
            case "HIGH",     "P2" -> "P2";
            case "MEDIUM",   "P3" -> "P3";
            default               -> null;   // LOW / unknown — no escalation matrix
        };
    }

    private String orDash(String val) {
        return (val != null && !val.isBlank()) ? val : "—";
    }

    private EscalationMatrixResponse toMatrixResponse(EscalationMatrixEntity e) {
        return EscalationMatrixResponse.builder()
                .uuid(e.getUuid())
                .priority(e.getPriority())
                .level(e.getLevel())
                .triggerHours(e.getTriggerHours())
                .emails(e.getEmails())
                .isActive(e.getIsActive())
                .updatedAt(e.getUpdatedAt())
                .build();
    }

    private EscalationLogResponse toLogResponse(TicketEscalationLogEntity e) {
        return EscalationLogResponse.builder()
                .uuid(e.getUuid())
                .ticketUuid(e.getTicketUuid())
                .ticketNumber(e.getTicketNumber())
                .priority(e.getPriority())
                .level(e.getLevel())
                .triggerHours(e.getTriggerHours())
                .emailsSent(e.getEmailsSent())
                .triggeredAt(e.getTriggeredAt())
                .build();
    }
}
