package com.pmis.ticket.controller;

import com.pmis.ticket.service.TicketService;
import com.pmis.ticket.web.request.*;
import com.pmis.ticket.web.response.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/tickets")
@Tag(name = "Ticket & SLA Management", description = "PMIS-FR-35/36/37 — Ticket lifecycle, SLA tracking, bulk operations")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    // ---- CRUD ---------------------------------------------------------------

    @PostMapping
    @Operation(summary = "Create a ticket (FR-35, FR-36, FR-37)")
    public ResponseEntity<TicketResponse> create(@RequestBody CreateTicketRequest req) {
        return ResponseEntity.status(201).body(ticketService.create(req));
    }

    @PatchMapping("/{uuid}")
    @Operation(summary = "Update status / assignee / fields — auto-writes comment on change")
    public ResponseEntity<TicketResponse> update(
            @PathVariable String uuid,
            @RequestBody UpdateTicketRequest req) {
        return ResponseEntity.ok(ticketService.update(uuid, req));
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "Full ticket detail with comments + attachments")
    public ResponseEntity<TicketResponse> detail(@PathVariable String uuid) {
        return ResponseEntity.ok(ticketService.getDetail(uuid));
    }

    // ---- SEARCH -------------------------------------------------------------

    @PostMapping("/_search")
    @Operation(summary = "Search tickets with filters + pagination")
    public ResponseEntity<SearchResponse> search(@RequestBody SearchTicketRequest req) {
        return ResponseEntity.ok(ticketService.search(req));
    }

    // ---- SLA ----------------------------------------------------------------

    @GetMapping("/sla-breached")
    @Operation(summary = "List all tickets that have breached their SLA deadline")
    public ResponseEntity<SearchResponse> slaBreached() {
        SearchTicketRequest req = SearchTicketRequest.builder()
                .criteria(SearchTicketRequest.SearchCriteria.builder()
                        .slaBreached(true).build())
                .build();
        return ResponseEntity.ok(ticketService.search(req));
    }

    @GetMapping("/sla-config")
    @Operation(summary = "List active SLA rules per category + priority")
    public ResponseEntity<List<SlaConfigResponse>> slaConfig() {
        return ResponseEntity.ok(ticketService.listSlaConfig());
    }

    // ---- BULK (FR-35.5) -----------------------------------------------------

    @PostMapping("/_bulk")
    @Operation(summary = "Bulk status update / assignment (FR-35.5)")
    public ResponseEntity<BulkOperationResponse> bulk(@RequestBody BulkOperationRequest req) {
        return ResponseEntity.accepted().body(ticketService.bulk(req));
    }

    @GetMapping("/_bulk/{bulkUuid}")
    @Operation(summary = "Poll bulk operation status")
    public ResponseEntity<BulkOperationResponse> bulkStatus(@PathVariable String bulkUuid) {
        return ResponseEntity.ok(ticketService.getBulkStatus(bulkUuid));
    }
}
