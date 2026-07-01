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

    @Override @Async
    public void notifyTicketCreated(TicketEntity ticket) {
        if (!props.isEnabled() || ticket.getReportedByEmail() == null) return;
        send(List.of(ticket.getReportedByEmail()), List.of(),
                "PMIS — Ticket " + ticket.getTicketNumber() + " Raised Successfully",
                buildCreatedBody(ticket), "TICKET_CREATED");
    }

    @Override @Async
    public void notifyTicketAssigned(TicketEntity ticket) {
        if (!props.isEnabled() || ticket.getAssigneeEmail() == null) return;
        List<String> cc = distinct(ticket.getReportedByEmail());
        send(List.of(ticket.getAssigneeEmail()), cc,
                "PMIS — Ticket " + ticket.getTicketNumber() + " Assigned to You",
                buildAssignedBody(ticket), "TICKET_ASSIGNED");
    }

    @Override @Async
    public void notifyTicketSentBack(TicketEntity ticket, String reason) {
        if (!props.isEnabled() || ticket.getReportedByEmail() == null) return;
        send(List.of(ticket.getReportedByEmail()), List.of(),
                "PMIS — Ticket " + ticket.getTicketNumber() + " Sent Back for Your Action",
                buildSentBackBody(ticket, reason), "TICKET_SENT_BACK");
    }

    @Override @Async
    public void notifyTicketResubmitted(TicketEntity ticket) {
        if (!props.isEnabled() || ticket.getAssigneeEmail() == null) return;
        send(List.of(ticket.getAssigneeEmail()), List.of(),
                "PMIS — Ticket " + ticket.getTicketNumber() + " Resubmitted for Action",
                buildResubmittedBody(ticket), "TICKET_RESUBMITTED");
    }

    @Override @Async
    public void notifyTicketResolved(TicketEntity ticket) {
        if (!props.isEnabled() || ticket.getReportedByEmail() == null) return;
        send(List.of(ticket.getReportedByEmail()), List.of(),
                "PMIS — Your Ticket " + ticket.getTicketNumber() + " Has Been Resolved",
                buildResolvedBody(ticket), "TICKET_RESOLVED");
    }

    @Override @Async
    public void notifyTicketReopened(TicketEntity ticket) {
        if (!props.isEnabled() || ticket.getAssigneeEmail() == null) return;
        send(List.of(ticket.getAssigneeEmail()), distinct(ticket.getReportedByEmail()),
                "PMIS — Ticket " + ticket.getTicketNumber() + " Reopened",
                buildReopenedBody(ticket), "TICKET_REOPENED");
    }

    @Override @Async
    public void notifyTicketClosed(TicketEntity ticket) {
        if (!props.isEnabled() || ticket.getReportedByEmail() == null) return;
        send(List.of(ticket.getReportedByEmail()), distinct(ticket.getAssigneeEmail()),
                "PMIS — Ticket " + ticket.getTicketNumber() + " Closed",
                buildClosedBody(ticket), "TICKET_CLOSED");
    }

    @Override @Async
    public void notifyTicketCancelled(TicketEntity ticket) {
        if (!props.isEnabled()) return;
        List<String> to = distinct(ticket.getReportedByEmail(), ticket.getAssigneeEmail());
        if (to.isEmpty()) return;
        send(to, List.of(),
                "PMIS — Ticket " + ticket.getTicketNumber() + " Cancelled",
                buildCancelledBody(ticket), "TICKET_CANCELLED");
    }

    @Override @Async
    public void notifySlaEscalation(TicketEntity ticket, String escalationLevel) {
        if (!props.isEnabled()) return;
        List<String> to = distinct(ticket.getAssigneeEmail(), ticket.getReportedByEmail());
        if (to.isEmpty()) return;
        send(to, List.of(), buildSlaSubject(ticket, escalationLevel),
                buildSlaBody(ticket, escalationLevel), "SLA_" + escalationLevel);
    }

    // =========================================================
    // HTTP transport
    // =========================================================

    private void send(List<String> to, List<String> cc, String subject, String body, String type) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("to",      to);
            payload.put("cc",      cc);
            payload.put("subject", subject);
            payload.put("body",    body);
            notificationRestClient.post().body(payload).retrieve().toBodilessEntity();
            log.info("Notification [{}] -> to={}", type, to);
        } catch (Exception ex) {
            log.error("Notification failed [{}] -> {}", type, ex.getMessage());
        }
    }

    // =========================================================
    // Body builders
    // =========================================================

    private String buildCreatedBody(TicketEntity t) {
        return """
                Hello %s,

                Your ticket has been raised successfully.

                  Ticket No    : %s
                  Title        : %s
                  Ticket Type  : %s
                  Category     : %s
                  Priority     : %s
                  Status       : OPEN
                  Project      : %s
                  Activity     : %s
                  Task         : %s
                  SLA By       : %s

                You will be notified as the ticket progresses.

                — PMIS Ticket System
                """.formatted(
                nvl(t.getReportedByName()), t.getTicketNumber(), t.getTitle(),
                nvl(t.getTicketType()), t.getCategory(), t.getPriority(),
                nvl(t.getProjectName()), nvl(t.getActivityName()), nvl(t.getTaskName()),
                epochLabel(t.getSlaDeadline()));
    }

    private String buildAssignedBody(TicketEntity t) {
        return """
                Hello %s,

                A ticket has been assigned to you for action.

                  Ticket No    : %s
                  Title        : %s
                  Ticket Type  : %s
                  Category     : %s
                  Priority     : %s
                  Status       : %s
                  Project      : %s
                  Activity     : %s
                  Task         : %s
                  Raised By    : %s
                  SLA By       : %s

                Please log in to PMIS to take action.

                — PMIS Ticket System
                """.formatted(
                nvl(t.getAssigneeName()), t.getTicketNumber(), t.getTitle(),
                nvl(t.getTicketType()), t.getCategory(), t.getPriority(), t.getStatus(),
                nvl(t.getProjectName()), nvl(t.getActivityName()), nvl(t.getTaskName()),
                nvl(t.getReportedByName()), epochLabel(t.getSlaDeadline()));
    }

    private String buildSentBackBody(TicketEntity t, String reason) {
        return """
                Hello %s,

                Your ticket has been sent back and requires your attention.

                  Ticket No    : %s
                  Title        : %s
                  Ticket Type  : %s
                  Category     : %s
                  Priority     : %s
                  Project      : %s
                  Activity     : %s
                  Task         : %s
                  Sent Back By : %s
                  Reason       : %s

                Please review the reason and resubmit the ticket with the required information.

                — PMIS Ticket System
                """.formatted(
                nvl(t.getReportedByName()), t.getTicketNumber(), t.getTitle(),
                nvl(t.getTicketType()), t.getCategory(), t.getPriority(),
                nvl(t.getProjectName()), nvl(t.getActivityName()), nvl(t.getTaskName()),
                nvl(t.getAssigneeName()), nvl(reason));
    }

    private String buildResubmittedBody(TicketEntity t) {
        return """
                Hello %s,

                Ticket %s has been resubmitted by the requester and is ready for action.

                  Title        : %s
                  Ticket Type  : %s
                  Category     : %s
                  Priority     : %s
                  Project      : %s
                  Activity     : %s
                  Task         : %s
                  Raised By    : %s
                  SLA By       : %s

                Please log in to PMIS to resume action.

                — PMIS Ticket System
                """.formatted(
                nvl(t.getAssigneeName()), t.getTicketNumber(), t.getTitle(),
                nvl(t.getTicketType()), t.getCategory(), t.getPriority(),
                nvl(t.getProjectName()), nvl(t.getActivityName()), nvl(t.getTaskName()),
                nvl(t.getReportedByName()), epochLabel(t.getSlaDeadline()));
    }

    private String buildResolvedBody(TicketEntity t) {
        return """
                Hello %s,

                Your ticket has been resolved.

                  Ticket No    : %s
                  Title        : %s
                  Ticket Type  : %s
                  Category     : %s
                  Priority     : %s
                  Project      : %s
                  Activity     : %s
                  Task         : %s
                  Resolved By  : %s
                  Resolved At  : %s

                If not satisfied, you may request to reopen this ticket.

                — PMIS Ticket System
                """.formatted(
                nvl(t.getReportedByName()), t.getTicketNumber(), t.getTitle(),
                nvl(t.getTicketType()), t.getCategory(), t.getPriority(),
                nvl(t.getProjectName()), nvl(t.getActivityName()), nvl(t.getTaskName()),
                nvl(t.getAssigneeName()), epochLabel(t.getResolvedAt()));
    }

    private String buildReopenedBody(TicketEntity t) {
        return """
                Hello %s,

                Ticket %s has been reopened and reassigned to you.

                  Title        : %s
                  Ticket Type  : %s
                  Category     : %s
                  Priority     : %s
                  Project      : %s
                  Activity     : %s
                  Task         : %s
                  Raised By    : %s
                  SLA By       : %s

                Please log in to PMIS to take action.

                — PMIS Ticket System
                """.formatted(
                nvl(t.getAssigneeName()), t.getTicketNumber(), t.getTitle(),
                nvl(t.getTicketType()), t.getCategory(), t.getPriority(),
                nvl(t.getProjectName()), nvl(t.getActivityName()), nvl(t.getTaskName()),
                nvl(t.getReportedByName()), epochLabel(t.getSlaDeadline()));
    }

    private String buildClosedBody(TicketEntity t) {
        return """
                Hello %s,

                Your ticket %s has been closed.

                  Title        : %s
                  Ticket Type  : %s
                  Category     : %s
                  Priority     : %s
                  Project      : %s
                  Activity     : %s
                  Task         : %s
                  Closed At    : %s

                Thank you for using PMIS.

                — PMIS Ticket System
                """.formatted(
                nvl(t.getReportedByName()), t.getTicketNumber(), t.getTitle(),
                nvl(t.getTicketType()), t.getCategory(), t.getPriority(),
                nvl(t.getProjectName()), nvl(t.getActivityName()), nvl(t.getTaskName()),
                epochLabel(t.getClosedAt()));
    }

    private String buildCancelledBody(TicketEntity t) {
        return """
                Hello,

                Ticket %s has been cancelled.

                  Title        : %s
                  Ticket Type  : %s
                  Category     : %s
                  Priority     : %s
                  Project      : %s
                  Activity     : %s
                  Task         : %s

                — PMIS Ticket System
                """.formatted(
                t.getTicketNumber(), t.getTitle(),
                nvl(t.getTicketType()), t.getCategory(), t.getPriority(),
                nvl(t.getProjectName()), nvl(t.getActivityName()), nvl(t.getTaskName()));
    }

    private String buildSlaSubject(TicketEntity t, String level) {
        return switch (level) {
            case "RISK_50"         -> "PMIS SLA Alert — 50% consumed: " + t.getTicketNumber();
            case "RISK_75"         -> "PMIS SLA Alert — 75% consumed: " + t.getTicketNumber();
            case "BREACH"          -> "PMIS SLA BREACH — Deadline missed: " + t.getTicketNumber();
            case "FIRST_RESPONSE"  -> "PMIS SLA BREACH — First response overdue: " + t.getTicketNumber();
            default                -> "PMIS SLA Escalation [" + level + "]: " + t.getTicketNumber();
        };
    }

    private String buildSlaBody(TicketEntity t, String level) {
        String detail = switch (level) {
            case "RISK_50"        -> "50% of the SLA budget has been consumed.";
            case "RISK_75"        -> "75% of the SLA budget has been consumed. Immediate action required.";
            case "BREACH"         -> "The resolution SLA deadline has been MISSED.";
            case "FIRST_RESPONSE" -> "No first response was recorded within the required window.";
            default               -> "SLA milestone triggered: " + level;
        };
        return """
                Hello,

                SLA alert for ticket %s.

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

                — PMIS Ticket System
                """.formatted(
                t.getTicketNumber(), t.getTitle(), t.getCategory(), t.getPriority(),
                nvl(t.getProjectName()), nvl(t.getActivityName()), t.getStatus(),
                nvl(t.getAssigneeName()), epochLabel(t.getSlaDeadline()), detail);
    }

    // =========================================================
    // Utilities
    // =========================================================

    private List<String> distinct(String... emails) {
        List<String> result = new ArrayList<>();
        for (String e : emails)
            if (e != null && !e.isBlank() && !result.contains(e)) result.add(e);
        return result;
    }

    private String nvl(String val) { return val != null ? val : "—"; }

    private String epochLabel(Long ms) {
        return ms != null ? new java.util.Date(ms).toString() : "N/A";
    }
}
