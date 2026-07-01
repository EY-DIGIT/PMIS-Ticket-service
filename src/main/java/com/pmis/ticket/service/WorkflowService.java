package com.pmis.ticket.service;

import com.pmis.ticket.workflow.WorkflowActionDef;
import com.pmis.ticket.workflow.WorkflowStateDef;

import java.util.List;

public interface WorkflowService {

    /** Validates the action against current state + user roles; returns the matched action def (contains nextState). */
    WorkflowActionDef validateAndGetAction(String currentState, String action, List<String> userRoles);

    /** Returns all valid actions for the given state and roles — useful for UI to show allowed buttons. */
    List<WorkflowActionDef> getAvailableActions(String currentState, List<String> userRoles);

    /** Returns the state definition for a given state name. */
    WorkflowStateDef getState(String state);
}
