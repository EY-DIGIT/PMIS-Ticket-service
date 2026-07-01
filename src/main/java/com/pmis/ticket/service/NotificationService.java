package com.pmis.ticket.service;

import com.pmis.ticket.entity.TicketEntity;

public interface NotificationService {

    void notifyTicketCreated(TicketEntity ticket);

    void notifyTicketAssigned(TicketEntity ticket);

    void notifyTicketSentBack(TicketEntity ticket, String reason);

    void notifyTicketResubmitted(TicketEntity ticket);

    void notifyTicketResolved(TicketEntity ticket);

    void notifyTicketReopened(TicketEntity ticket);

    void notifyTicketClosed(TicketEntity ticket);

    void notifyTicketCancelled(TicketEntity ticket);

    void notifySlaEscalation(TicketEntity ticket, String escalationLevel);
}
