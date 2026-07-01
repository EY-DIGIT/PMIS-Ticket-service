package com.pmis.ticket.service;

import com.pmis.ticket.web.request.EscalationMatrixRequest;
import com.pmis.ticket.web.response.EscalationLogResponse;
import com.pmis.ticket.web.response.EscalationMatrixResponse;

import java.util.List;

public interface EscalationService {

    List<EscalationMatrixResponse> listMatrix();

    EscalationMatrixResponse updateMatrix(String uuid, EscalationMatrixRequest req);

    List<EscalationLogResponse> getLogsForTicket(String ticketUuid);
}
