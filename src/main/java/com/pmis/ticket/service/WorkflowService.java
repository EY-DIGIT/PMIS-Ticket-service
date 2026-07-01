package com.pmis.ticket.service;

import com.pmis.ticket.workflow.WorkflowActionDef;
import com.pmis.ticket.workflow.WorkflowDefinition;
import com.pmis.ticket.workflow.WorkflowStateDef;

import java.util.List;

public interface WorkflowService {

    /** Save / update workflow JSON to DB and hot-reload in memory. */
    WorkflowDefinition saveWorkflow(String workflowJson, String updatedBy);

    /** Get the current active workflow definition. */
    WorkflowDefinition getWorkflow();

    /** Validate action against current state + roles; returns action def with nextState. */
    WorkflowActionDef validateAndGetAction(String currentState, String action, List<String> userRoles);

    /** Returns allowed actions for given state + roles (for UI button visibility). */
    List<WorkflowActionDef> getAvailableActions(String currentState, List<String> userRoles);

    /** Returns state definition for a given state name. */
    WorkflowStateDef getState(String state);
}
