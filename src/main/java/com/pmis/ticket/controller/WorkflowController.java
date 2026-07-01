package com.pmis.ticket.controller;

import com.pmis.ticket.service.WorkflowService;
import com.pmis.ticket.workflow.WorkflowActionDef;
import com.pmis.ticket.workflow.WorkflowDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/workflow")
@Tag(name = "Workflow Config", description = "Manage ticket workflow definition stored in DB")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;

    @GetMapping
    @Operation(summary = "Get the current active workflow definition")
    public ResponseEntity<WorkflowDefinition> getWorkflow() {
        return ResponseEntity.ok(workflowService.getWorkflow());
    }

    @PostMapping
    @Operation(summary = "Save / update the workflow JSON and hot-reload without restart",
               description = "Pass the full workflow JSON in the request body as a raw string. " +
                             "The new workflow is validated and applied immediately in memory.")
    public ResponseEntity<WorkflowDefinition> saveWorkflow(
            @RequestBody String workflowJson,
            @RequestParam(defaultValue = "SYSTEM") String updatedBy) {
        return ResponseEntity.ok(workflowService.saveWorkflow(workflowJson, updatedBy));
    }

    @GetMapping("/actions")
    @Operation(summary = "Get available actions for a state + role (useful for UI button visibility)")
    public ResponseEntity<List<WorkflowActionDef>> getAvailableActions(
            @RequestParam String currentState,
            @RequestParam List<String> roles) {
        return ResponseEntity.ok(workflowService.getAvailableActions(currentState, roles));
    }
}
