package com.pmis.ticket.controller;

import com.pmis.ticket.service.EscalationService;
import com.pmis.ticket.web.request.EscalationMatrixRequest;
import com.pmis.ticket.web.response.EscalationLogResponse;
import com.pmis.ticket.web.response.EscalationMatrixResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/escalation")
@Tag(name = "Escalation Matrix", description = "Manage SLA escalation rules and view escalation history")
@RequiredArgsConstructor
public class EscalationController {

    private final EscalationService escalationService;

    @GetMapping("/matrix")
    @Operation(summary = "List all active escalation matrix rules (P1/P2/P3 × L1/L2/L3)")
    public ResponseEntity<List<EscalationMatrixResponse>> listMatrix() {
        return ResponseEntity.ok(escalationService.listMatrix());
    }

    @PatchMapping("/matrix/{uuid}")
    @Operation(summary = "Update trigger hours or emails for a matrix entry")
    public ResponseEntity<EscalationMatrixResponse> updateMatrix(
            @PathVariable String uuid,
            @RequestBody EscalationMatrixRequest req) {
        return ResponseEntity.ok(escalationService.updateMatrix(uuid, req));
    }

    @GetMapping("/tickets/{ticketUuid}/logs")
    @Operation(summary = "Get escalation history for a specific ticket")
    public ResponseEntity<List<EscalationLogResponse>> getLogs(@PathVariable String ticketUuid) {
        return ResponseEntity.ok(escalationService.getLogsForTicket(ticketUuid));
    }
}
