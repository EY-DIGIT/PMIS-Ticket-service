package com.pmis.ticket.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmis.ticket.service.WorkflowService;
import com.pmis.ticket.workflow.WorkflowActionDef;
import com.pmis.ticket.workflow.WorkflowDefinition;
import com.pmis.ticket.workflow.WorkflowStateDef;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {

    private final ObjectMapper objectMapper;

    private Map<String, WorkflowStateDef> stateMap;

    @PostConstruct
    public void loadWorkflow() {
        try {
            ClassPathResource resource = new ClassPathResource("pmis_ticket_workflow.json");
            WorkflowDefinition def = objectMapper.readValue(resource.getInputStream(), WorkflowDefinition.class);
            stateMap = def.getStates().stream()
                    .collect(Collectors.toMap(
                            s -> s.getState() == null ? "__START__" : s.getState(),
                            Function.identity()
                    ));
            log.info("Workflow loaded: {} states", stateMap.size());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load pmis_ticket_workflow.json", e);
        }
    }

    @Override
    public WorkflowActionDef validateAndGetAction(String currentState, String action, List<String> userRoles) {
        String stateKey = currentState == null ? "__START__" : currentState;
        WorkflowStateDef stateDef = stateMap.get(stateKey);

        if (stateDef == null) {
            throw new IllegalArgumentException("Unknown workflow state: " + currentState);
        }
        if (stateDef.isTerminateState()) {
            throw new IllegalArgumentException(
                    "Ticket is in terminal state '" + currentState + "'. No further actions allowed.");
        }

        return stateDef.getActions().stream()
                .filter(a -> a.getAction().equals(action))
                .findFirst()
                .map(a -> {
                    boolean hasRole = userRoles.stream().anyMatch(r -> a.getRoles().contains(r));
                    if (!hasRole) {
                        throw new IllegalArgumentException(
                                "Action '" + action + "' is not allowed for your role. " +
                                "Required: " + a.getRoles());
                    }
                    return a;
                })
                .orElseThrow(() -> new IllegalArgumentException(
                        "Action '" + action + "' is not valid in state '" + currentState + "'. " +
                        "Valid actions: " + stateDef.getActions().stream()
                                .map(WorkflowActionDef::getAction).toList()));
    }

    @Override
    public List<WorkflowActionDef> getAvailableActions(String currentState, List<String> userRoles) {
        String stateKey = currentState == null ? "__START__" : currentState;
        WorkflowStateDef stateDef = stateMap.get(stateKey);
        if (stateDef == null || stateDef.isTerminateState()) return List.of();

        return stateDef.getActions().stream()
                .filter(a -> userRoles.stream().anyMatch(r -> a.getRoles().contains(r)))
                .toList();
    }

    @Override
    public WorkflowStateDef getState(String state) {
        WorkflowStateDef def = stateMap.get(state == null ? "__START__" : state);
        if (def == null) throw new NoSuchElementException("Unknown workflow state: " + state);
        return def;
    }
}
