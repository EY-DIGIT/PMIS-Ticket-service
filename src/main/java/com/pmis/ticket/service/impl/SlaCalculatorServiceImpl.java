package com.pmis.ticket.service.impl;

import com.pmis.ticket.entity.TicketEntity;
import com.pmis.ticket.entity.WorkingCalendarEntity;
import com.pmis.ticket.service.SlaCalculatorService;
import org.springframework.stereotype.Service;

import java.time.*;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class SlaCalculatorServiceImpl implements SlaCalculatorService {

    @Override
    public long computeDeadline(long startMs, int hours, String clockType, WorkingCalendarEntity cal) {
        if (!"BUSINESS_HOURS".equals(clockType) || cal == null) {
            return startMs + ((long) hours * 3_600_000L);
        }
        return addBusinessHours(startMs, hours, cal);
    }

    @Override
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

    @Override
    public boolean isFirstResponseBreached(TicketEntity t, long now) {
        return t.getFirstResponseAt() == null
                && t.getFirstResponseDeadline() != null
                && now >= t.getFirstResponseDeadline();
    }

    @Override
    public Long firstResponseRemainingMs(TicketEntity t, long now) {
        if (t.getFirstResponseAt() != null || t.getFirstResponseDeadline() == null) return null;
        long remaining = t.getFirstResponseDeadline() - now;
        return remaining > 0 ? remaining : null;
    }

    @Override
    public Long slaRemainingMs(TicketEntity t, long now) {
        if (t.getSlaDeadline() == null || Boolean.TRUE.equals(t.getSlaBreached())) return null;
        long effectiveDeadline = (t.getSlaPausedAt() != null)
                ? t.getSlaDeadline() + (now - t.getSlaPausedAt())
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
        while (!workDays.contains(dt.getDayOfWeek()) || holidays.contains(dt.toLocalDate())) {
            dt = dt.toLocalDate().plusDays(1).atTime(dayStart, 0).atZone(dt.getZone());
        }
        if (dt.getHour() < dayStart) {
            dt = dt.withHour(dayStart).withMinute(0).withSecond(0).withNano(0);
        }
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
