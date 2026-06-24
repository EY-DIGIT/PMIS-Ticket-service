package com.pmis.ticket.service;

import com.pmis.ticket.entity.TicketEntity;
import com.pmis.ticket.entity.WorkingCalendarEntity;

public interface SlaCalculatorService {

    long computeDeadline(long startMs, int hours, String clockType, WorkingCalendarEntity cal);

    String computeSlaStatus(TicketEntity t, long now);

    boolean isFirstResponseBreached(TicketEntity t, long now);

    Long firstResponseRemainingMs(TicketEntity t, long now);

    Long slaRemainingMs(TicketEntity t, long now);
}
