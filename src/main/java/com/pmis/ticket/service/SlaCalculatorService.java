package com.pmis.ticket.service;

import com.pmis.ticket.entity.TicketEntity;
import com.pmis.ticket.entity.WorkingCalendarEntity;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Pure SLA math: deadline calculation (calendar vs business hours),
 * SLA status percentage, and first-response status.
 * Stateless — no DB access.
 */
@Service
public class SlaCalculatorService {

    // =========================================================
    // Deadline computation
    // =========================================================

    /**
     * Compute the absolute epoch-ms deadline from a start time.
     *
     * @param startMs   epoch ms the SLA clock starts
     * @param hours     SLA budget in hours
     * @param clockType "BUSINESS_HOURS" or "CALENDAR_HOURS"
     * @param cal       working-calendar (required for BUSINESS_HOURS; null falls back to CALENDAR_HOURS)
     */
    public long computeDeadline(long startMs, int hours, String clockType, WorkingCalendarEntity cal) {
        if (!"BUSINESS_HOURS".equals(clockType) || cal == null) {
            return startMs + ((long) hours * 3_600_000L);
        }
        return addBusinessHours(startMs, hours, cal);
    }

    // =========================================================
    // SLA status
    // =========================================================

    /**
     * Compute the current resolution-SLA status string for a ticket.
     * <p>
     * Status values:
     *   PAUSED     – ticket is on hold (clock frozen)
     *   BREACHED   – deadline has passed
     *   AT_RISK_75 – ≥ 75 % of SLA budget consumed
     *   AT_RISK_50 – ≥ 50 % of SLA budget consumed
     *   ON_TRACK   – < 50 % consumed
     *   NO_SLA     – no deadline configured
     */
    public String computeSlaStatus(TicketEntity t, long now) {
        if (t.getSlaDeadline() == null) return "NO_SLA";
        if (t.getSlaPausedAt() != null)  return "PAUSED";
        if (now >= t.getSlaDeadline())   return "BREACHED";

        long paused          = orZero(t.getTotalPausedMs());
        long originalBudget  = t.getSlaDeadline() - t.getCreatedAt() - paused;
        long effectiveElapsed = now - t.getCreatedAt() - paused;

        if (originalBudget <= 0) return "BREACHED";
        double pct = (double) effectiveElapsed / originalBudget;
        if (pct >= 0.75) return "AT_RISK_75";
        if (pct >= 0.50) return "AT_RISK_50";
        return "ON_TRACK";
    }

    /**
     * Returns true when the first-response deadline has passed and no response has been recorded.
     */
    public boolean isFirstResponseBreached(TicketEntity t, long now) {
        return t.getFirstResponseAt() == null
                && t.getFirstResponseDeadline() != null
                && now >= t.getFirstResponseDeadline();
    }

    /**
     * Compute remaining ms for the first-response SLA.
     * Returns null if already responded, no deadline set, or already breached.
     */
    public Long firstResponseRemainingMs(TicketEntity t, long now) {
        if (t.getFirstResponseAt() != null || t.getFirstResponseDeadline() == null) return null;
        long remaining = t.getFirstResponseDeadline() - now;
        return remaining > 0 ? remaining : null;
    }

    /**
     * Remaining ms for the resolution SLA.
     * Accounts for the current pause: while paused the deadline is frozen
     * at (slaDeadline - slaPausedAt).
     */
    public Long slaRemainingMs(TicketEntity t, long now) {
        if (t.getSlaDeadline() == null || Boolean.TRUE.equals(t.getSlaBreached())) return null;
        // While paused: time stopped at the moment of pause
        long effectiveDeadline = (t.getSlaPausedAt() != null)
                ? t.getSlaDeadline() + (now - t.getSlaPausedAt())  // deadline not yet extended
                : t.getSlaDeadline();
        long remaining = effectiveDeadline - now;
        return remaining > 0 ? remaining : null;
    }

    // =========================================================
    // Business-hours arithmetic
    // =========================================================

    private long addBusinessHours(long startEpochMs, int hours, WorkingCalendarEntity cal) {
        ZoneId zone             = ZoneId.of(cal.getTimezone() != null ? cal.getTimezone() : "UTC");
        Set<DayOfWeek> workDays = parseWorkDays(cal.getWorkDays());
        Set<LocalDate> holidays = parseHolidays(cal.getHolidays());
        int dayStart            = cal.getWorkDayStart() != null ? cal.getWorkDayStart() : 9;
        int dayEnd              = cal.getWorkDayEnd()   != null ? cal.getWorkDayEnd()   : 18;

        ZonedDateTime dt = Instant.ofEpochMilli(startEpochMs).atZone(zone);

        // Snap forward to the next open business hour
        dt = snapToBusinessHours(dt, workDays, holidays, dayStart, dayEnd);

        int remaining = hours;
        while (remaining > 0) {
            int hoursLeftToday = dayEnd - dt.getHour();
            if (hoursLeftToday <= 0) {
                dt = nextBusinessDayStart(dt, workDays, holidays, dayStart);
                continue;
            }
            if (remaining <= hoursLeftToday) {
                dt = dt.plusHours(remaining);
                remaining = 0;
            } else {
                remaining -= hoursLeftToday;
                dt = nextBusinessDayStart(dt, workDays, holidays, dayStart);
            }
        }
        return dt.toInstant().toEpochMilli();
    }

    private ZonedDateTime snapToBusinessHours(ZonedDateTime dt, Set<DayOfWeek> workDays,
                                               Set<LocalDate> holidays, int dayStart, int dayEnd) {
        // Skip non-working days
        while (!workDays.contains(dt.getDayOfWeek()) || holidays.contains(dt.toLocalDate())) {
            dt = dt.toLocalDate().plusDays(1).atTime(dayStart, 0).atZone(dt.getZone());
        }
        // Before start of day → snap to start
        if (dt.getHour() < dayStart) {
            dt = dt.withHour(dayStart).withMinute(0).withSecond(0).withNano(0);
        }
        // After end of day → next business day
        if (dt.getHour() >= dayEnd) {
            dt = nextBusinessDayStart(dt, workDays, holidays, dayStart);
        }
        return dt;
    }

    private ZonedDateTime nextBusinessDayStart(ZonedDateTime dt, Set<DayOfWeek> workDays,
                                                Set<LocalDate> holidays, int dayStart) {
        dt = dt.toLocalDate().plusDays(1).atTime(dayStart, 0).atZone(dt.getZone());
        while (!workDays.contains(dt.getDayOfWeek()) || holidays.contains(dt.toLocalDate())) {
            dt = dt.toLocalDate().plusDays(1).atTime(dayStart, 0).atZone(dt.getZone());
        }
        return dt;
    }

    // =========================================================
    // Parsers
    // =========================================================

    private Set<DayOfWeek> parseWorkDays(String workDays) {
        if (workDays == null || workDays.isBlank()) {
            return Set.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                          DayOfWeek.THURSDAY, DayOfWeek.FRIDAY);
        }
        return Arrays.stream(workDays.split(","))
                .map(String::trim)
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toSet());
    }

    /** Parses JSON array string like ["2025-01-26","2025-08-15"] into LocalDate set. */
    private Set<LocalDate> parseHolidays(String holidays) {
        if (holidays == null || holidays.isBlank()) return Set.of();
        String cleaned = holidays.replaceAll("[\\[\\]\"\\s]", "");
        if (cleaned.isBlank()) return Set.of();
        return Arrays.stream(cleaned.split(","))
                .filter(s -> !s.isBlank())
                .map(LocalDate::parse)
                .collect(Collectors.toSet());
    }

    private long orZero(Long val) {
        return val != null ? val : 0L;
    }
}
