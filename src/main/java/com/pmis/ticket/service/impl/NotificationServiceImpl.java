package com.pmis.ticket.service.impl;

import com.pmis.ticket.config.NotificationProperties;
import com.pmis.ticket.entity.TicketEntity;
import com.pmis.ticket.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationProperties props;
    private final RestClient             notificationRestClient;

    @Override
    @Async
    public void notifyTicketCreated(TicketEntity ticket) {
        if (!props.isEnabled() || ticket.getAssigneeEmail() == null) return;
        send(
            List.of(ticket.getAssigneeEmail()),
            List.of(),
            "PMIS — New Ticket Assigned: " + ticket.getTicketNumber(),
            buildTicketCreatedBody(ticket),
            "TICKET_CREATED"
        );
    }

    @Override
    @Async
    public void notifyTicketAssigned(TicketEntity ticket) {
        if (!props.isEnabled() || ticket.getAssigneeEmail() == null) return;

        List<String> cc = new ArrayList<>();
        if (ticket.getReportedByEmail() != null
                && !ticket.getReportedByEmail().equals(ticket.getAssigneeEmail())) {
            cc.add(ticket.getReportedByEmail());
        }
        send(
            List.of(ticket.getAssigneeEmail()),
            cc,
            "PMIS — Ticket " + ticket.getTicketNumber() + " Assigned to You",
            buildAssignedBody(ticket),
            "TICKET_ASSIGNED"
        );
    }

    @Override
    @Async
    public void notifyStatusChanged(TicketEntity ticket, String previousStatus) {
        if (!props.isEnabled()) return;

        List<String> to = distinct(ticket.getReportedByEmail(), ticket.getAssigneeEmail());
        if (to.isEmpty()) return;
        send(
            to,
            List.of(),
            "PMIS — Ticket " + ticket.getTicketNumber() + " Status: "
                    + previousStatus + " -> " + ticket.getStatus(),
            buildStatusChangedBody(ticket, previousStatus),
            "TICKET_STATUS_CHANGED"
        );
    }

    @Override
    @Async
    public void notifyTicketResolved(TicketEntity ticket) {
        if (!props.isEnabled() || ticket.getReportedByEmail() == null) return;
        send(
            List.of(ticket.getReportedByEmail()),
            List.of(),
            "PMIS — Your Ticket " + ticket.getTicketNumber() + " Has Been Resolved",
            buildTicketResolvedBody(ticket),
            "TICKET_RESOLVED"
        );
    }

    @Override
    @Async
    public void notifySlaEscalation(TicketEntity ticket, String escalationLevel) {
        if (!props.isEnabled()) return;

        List<String> to = distinct(ticket.getAssigneeEmail(), ticket.getReportedByEmail());
        if (to.isEmpty()) return;
        send(
            to,
            List.of(),
            buildSlaSubject(ticket, escalationLevel),
            buildSlaBody(ticket, escalationLevel),
            "SLA_" + escalationLevel
        );
    }

    // =========================================================
    // HTTP transport
    // =========================================================

    private void send(List<String> to, List<String> cc,
                      String subject, String body, String notificationType) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("to",      to);
            payload.put("cc",      cc);
            payload.put("subject", subject);
            payload.put("body",    body);

            notificationRestClient.post()
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Notification dispatched [{}] -> to={}", notificationType, to);

        } catch (Exception ex) {
            log.error("Notification failed [{}] -> to={} : {}", notificationType, to, ex.getMessage());
        }
    }

    // =========================================================
    // Body builders
    // =========================================================

    private String buildTicketCreatedBody(TicketEntity t) {
        return """
                Hello %s,

                A new ticket has been created and assigned to you.

                  Ticket No    : %s
                  Title        : %s
                  Category     : %s
                  Priority     : %s
                  Status       : %s
                  Project      : %s
                  Activity     : %s
                  Task         : %s
                  SLA By       : %s

                Please log in to PMIS to view and action the ticket.

                Regards,
                PMIS System
                """.formatted(
                nvl(t.getAssigneeName()),
                t.getTicketNumber(),
                t.getTitle(),
                t.getCategory(),
                t.getPriority(),
                t.getStatus(),
                nvl(t.getProjectName()),
                nvl(t.getActivityName()),
                nvl(t.getTaskName()),
                epochLabel(t.getSlaDeadline()));
    }

    private String buildAssignedBody(TicketEntity t) {
        return """
                Hello %s,

                Ticket %s has been assigned to you.

                  Title        : %s
                  Category     : %s
                  Priority     : %s
                  Status       : %s
                  Project      : %s
                  Activity     : %s
                  Task         : %s
                  SLA By       : %s

                Please log in to PMIS to begin working on this ticket.

                Regards,
                PMIS System
                """.formatted(
                nvl(t.getAssigneeName()),
                t.getTicketNumber(),
                t.getTitle(),
                t.getCategory(),
                t.getPriority(),
                t.getStatus(),
                nvl(t.getProjectName()),
                nvl(t.getActivityName()),
                nvl(t.getTaskName()),
                epochLabel(t.getSlaDeadline()));
    }

    private String buildStatusChangedBody(TicketEntity t, String previousStatus) {
        return """
                Hello,

                The status of ticket %s has changed.

                  Title        : %s
                  Category     : %s
                  Priority     : %s
                  Project      : %s
                  Activity     : %s
                  Previous     : %s
                  New Status   : %s
                  Updated At   : %s

                Regards,
                PMIS System
                """.formatted(
                t.getTicketNumber(),
                t.getTitle(),
                t.getCategory(),
                t.getPriority(),
                nvl(t.getProjectName()),
                nvl(t.getActivityName()),
                previousStatus,
                t.getStatus(),
                epochLabel(t.getUpdatedAt()));
    }

    private String buildTicketResolvedBody(TicketEntity t) {
        return """
                Hello %s,

                Your ticket has been resolved.

                  Ticket No    : %s
                  Title        : %s
                  Category     : %s
                  Priority     : %s
                  Project      : %s
                  Activity     : %s
                  Task         : %s
                  Resolved By  : %s
                  Resolved At  : %s

                If you feel this ticket has not been resolved to your satisfaction,
                please reopen it by contacting the support team.

                Regards,
                PMIS System
                """.formatted(
                nvl(t.getReportedByName()),
                t.getTicketNumber(),
                t.getTitle(),
                t.getCategory(),
                t.getPriority(),
                nvl(t.getProjectName()),
                nvl(t.getActivityName()),
                nvl(t.getTaskName()),
                nvl(t.getAssigneeName()),
                epochLabel(t.getResolvedAt()));
    }

    private String buildSlaSubject(TicketEntity t, String level) {
        return switch (level) {
            case "RISK_50"         -> "PMIS SLA Alert - 50%% consumed: " + t.getTicketNumber();
            case "RISK_75"         -> "PMIS SLA Alert - 75%% consumed: " + t.getTicketNumber();
            case "BREACH"          -> "PMIS SLA BREACH - Resolution deadline missed: " + t.getTicketNumber();
            case "FIRST_RESPONSE"  -> "PMIS SLA BREACH - First-response deadline missed: " + t.getTicketNumber();
            case "POST_BREACH_24H" -> "PMIS SLA - 24h post-breach, ticket still open: " + t.getTicketNumber();
            default                -> "PMIS SLA Escalation [" + level + "]: " + t.getTicketNumber();
        };
    }

    private String buildSlaBody(TicketEntity t, String level) {
        String detail = switch (level) {
            case "RISK_50"         -> "50%% of the SLA budget has been consumed.";
            case "RISK_75"         -> "75%% of the SLA budget has been consumed. Immediate action required.";
            case "BREACH"          -> "The resolution SLA deadline has been MISSED. The ticket is now in breach.";
            case "FIRST_RESPONSE"  -> "No first response was recorded within the required window. SLA breached.";
            case "POST_BREACH_24H" -> "The ticket has been in breach for more than 24 hours without resolution.";
            default                -> "SLA milestone triggered: " + level;
        };

        return """
                Hello,

                SLA escalation for ticket %s.

                  Title        : %s
                  Category     : %s
                  Priority     : %s
                  Project      : %s
                  Activity     : %s
                  Status       : %s
                  Assignee     : %s
                  SLA By       : %s

                %s

                Please take immediate action in PMIS.

                Regards,
                PMIS System
                """.formatted(
                t.getTicketNumber(),
                t.getTitle(),
                t.getCategory(),
                t.getPriority(),
                nvl(t.getProjectName()),
                nvl(t.getActivityName()),
                t.getStatus(),
                nvl(t.getAssigneeName()),
                epochLabel(t.getSlaDeadline()),
                detail);
    }

    // =========================================================
    // Utilities
    // =========================================================

    private List<String> distinct(String... emails) {
        List<String> result = new ArrayList<>();
        for (String e : emails) {
            if (e != null && !e.isBlank() && !result.contains(e)) result.add(e);
        }
        return result;
    }

    private String nvl(String val) {
        return val != null ? val : "";
    }

    private String epochLabel(Long epochMs) {
        if (epochMs == null) return "N/A";
        return new java.util.Date(epochMs).toString();
    }
}
