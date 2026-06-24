package com.pmis.ticket.service;

import com.pmis.ticket.entity.TicketEntity;

public interface NotificationService {

    void notifyTicketCreated(TicketEntity ticket);

    void notifyTicketAssigned(TicketEntity ticket);

    void notifyStatusChanged(TicketEntity ticket, String previousStatus);

    void notifyTicketResolved(TicketEntity ticket);

    void notifySlaEscalation(TicketEntity ticket, String escalationLevel);
}
