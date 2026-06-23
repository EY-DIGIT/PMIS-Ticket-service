package com.pmis.ticket.service;

import com.pmis.ticket.config.NotificationProperties;
import com.pmis.ticket.entity.TicketEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Sends outbound email notifications to the external notification API.
 *
 * Every public method is @Async — the call returns immediately and the HTTP
 * request is fired on the "notif-*" thread-pool (configured in application.properties).
 * Any API failure is caught and logged; it never propagates to the caller.
 *
 * API contract expected by the remote endpoint:
 * POST {app.notification.url}
 * Headers: Authorization: <token>   (omitted when token is blank)
 *          Content-Type: application/json
 * Body:
 * {
 *   "to":               ["a@example.com"],
 *   "cc":               [],
 *   "subject":          "...",
 *   "body":             "plain-text email body",
 *   "notificationType": "TICKET_ASSIGNED"
 * }
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationProperties props;
    private final RestClient             notificationRestClient;

    // =========================================================
    // Ticket lifecycle events
    // =========================================================

    /** Fired when a ticket is created and already has an assignee. */
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

    /** Fired when an existing ticket is (re-)assigned to a new owner. */
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

    /** Fired when the ticket status changes (both reporter and assignee are notified). */
    @Async
    public void notifyStatusChanged(TicketEntity ticket, String previousStatus) {
        if (!props.isEnabled()) return;

        List<String> to  = distinct(ticket.getReportedByEmail(), ticket.getAssigneeEmail());
        if (to.isEmpty()) return;

        send(
            to,
            List.of(),
            "PMIS — Ticket " + ticket.getTicketNumber() + " Status: "
                    + previousStatus + " → " + ticket.getStatus(),
            buildStatusChangedBody(ticket, previousStatus),
            "TICKET_STATUS_CHANGED"
        );
    }

    // =========================================================
    // SLA escalation events
    // =========================================================

    /**
     * Fired for each SLA milestone:
     *   RISK_50        – 50 % SLA budget consumed
     *   RISK_75        – 75 % SLA budget consumed
     *   BREACH         – resolution deadline passed
     *   FIRST_RESPONSE – first-response deadline passed
     *   POST_BREACH_24H – 24 h after resolution breach
     */
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
            payload.put("to",               to);
            payload.put("cc",               cc);
            payload.put("subject",          subject);
            payload.put("body",             body);
            payload.put("notificationType", notificationType);

            notificationRestClient.post()
                    .body(payload)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Notification dispatched [{}] → to={}", notificationType, to);

        } catch (Exception ex) {
            log.error("Notification failed [{}] → to={} : {}", notificationType, to, ex.getMessage());
        }
    }

    // =========================================================
    // Body builders
    // =========================================================

    private String buildTicketCreatedBody(TicketEntity t) {
        return """
                Hello %s,

                A new ticket has been created and assigned to you.

                  Ticket No : %s
                  Title     : %s
                  Category  : %s
                  Priority  : %s
                  Status    : %s
                  SLA By    : %s

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
                epochLabel(t.getSlaDeadline()));
    }

    private String buildAssignedBody(TicketEntity t) {
        return """
                Hello %s,

                Ticket %s has been assigned to you.

                  Title     : %s
                  Category  : %s
                  Priority  : %s
                  Status    : %s
                  SLA By    : %s

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
                epochLabel(t.getSlaDeadline()));
    }

    private String buildStatusChangedBody(TicketEntity t, String previousStatus) {
        return """
                Hello,

                The status of ticket %s has changed.

                  Title     : %s
                  Category  : %s
                  Priority  : %s
                  Previous  : %s
                  New Status: %s
                  Updated At: %s

                Regards,
                PMIS System
                """.formatted(
                t.getTicketNumber(),
                t.getTitle(),
                t.getCategory(),
                t.getPriority(),
                previousStatus,
                t.getStatus(),
                epochLabel(t.getUpdatedAt()));
    }

    private String buildSlaSubject(TicketEntity t, String level) {
        return switch (level) {
            case "RISK_50"        -> "⚠ PMIS SLA Alert — 50%% consumed: " + t.getTicketNumber();
            case "RISK_75"        -> "⚠ PMIS SLA Alert — 75%% consumed: " + t.getTicketNumber();
            case "BREACH"         -> "🚨 PMIS SLA BREACH — Resolution deadline missed: " + t.getTicketNumber();
            case "FIRST_RESPONSE" -> "🚨 PMIS SLA BREACH — First-response deadline missed: " + t.getTicketNumber();
            case "POST_BREACH_24H"-> "🚨 PMIS SLA — 24 h post-breach, ticket still open: " + t.getTicketNumber();
            default               -> "PMIS SLA Escalation [" + level + "]: " + t.getTicketNumber();
        };
    }

    private String buildSlaBody(TicketEntity t, String level) {
        String detail = switch (level) {
            case "RISK_50"        -> "50 %% of the SLA budget has been consumed.";
            case "RISK_75"        -> "75 %% of the SLA budget has been consumed. Immediate action required.";
            case "BREACH"         -> "The resolution SLA deadline has been MISSED. The ticket is now in breach.";
            case "FIRST_RESPONSE" -> "No first response was recorded within the required window. SLA breached.";
            case "POST_BREACH_24H"-> "The ticket has been in breach for more than 24 hours without resolution.";
            default               -> "SLA milestone triggered: " + level;
        };

        return """
                Hello,

                SLA escalation for ticket %s.

                  Title     : %s
                  Category  : %s
                  Priority  : %s
                  Status    : %s
                  Assignee  : %s
                  SLA By    : %s

                %s

                Please take immediate action in PMIS.

                Regards,
                PMIS System
                """.formatted(
                t.getTicketNumber(),
                t.getTitle(),
                t.getCategory(),
                t.getPriority(),
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
