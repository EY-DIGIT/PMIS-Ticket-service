package com.pmis.ticket.service;

import com.pmis.ticket.web.request.*;
import com.pmis.ticket.web.response.*;

import java.util.List;

public interface TicketService {

    TicketResponse create(CreateTicketRequest req);

    TicketResponse update(String uuid, UpdateTicketRequest req);

    TicketResponse getDetail(String uuid);

    SearchResponse search(SearchTicketRequest req);

    BulkOperationResponse bulk(BulkOperationRequest req);

    BulkOperationResponse getBulkStatus(String bulkUuid);

    TicketCountsResponse getCounts(String projectId, String activityId, String taskId,
                                   Long fromDate, Long toDate);

    SlaConfigResponse createSlaConfig(SlaConfigRequest req);

    SlaConfigResponse updateSlaConfig(String uuid, SlaConfigRequest req);

    List<SlaConfigResponse> listSlaConfig();

    WorkingCalendarResponse createCalendar(WorkingCalendarRequest req);

    WorkingCalendarResponse updateCalendar(String uuid, WorkingCalendarRequest req);

    List<WorkingCalendarResponse> listCalendars();
}
