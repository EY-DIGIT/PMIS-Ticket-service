package com.pmis.ticket.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pmis.ticket.entity.WorkflowConfigEntity;
import com.pmis.ticket.repository.WorkflowConfigRepository;
import com.pmis.ticket.service.WorkflowService;
import com.pmis.ticket.workflow.WorkflowActionDef;
import com.pmis.ticket.workflow.WorkflowDefinition;
import com.pmis.ticket.workflow.WorkflowStateDef;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class WorkflowServiceImpl implements WorkflowService {

    private static final String BUSINESS_SERVICE = "PMIS-TICKET";

    private final ObjectMapper               objectMapper;
    private final WorkflowConfigRepository   workflowConfigRepo;

    private Map<String, WorkflowStateDef> stateMap;

    // =========================================================
    // Startup — load from DB, fall back to file
    // =========================================================

    @PostConstruct
    public void loadWorkflow() {
        workflowConfigRepo.findByBusinessServiceAndIsActiveTrue(BUSINESS_SERVICE)
                .ifPresentOrElse(
                        entity -> {
                            parseAndApply(entity.getWorkflowJson());
                            log.info("Workflow loaded from DB for '{}'", BUSINESS_SERVICE);
                        },
                        () -> {
                            String json = loadFromFile();
                            saveToDb(json, "SYSTEM");        // auto-seed DB on first start
                            parseAndApply(json);
                            log.info("Workflow seeded from file into DB for '{}'", BUSINESS_SERVICE);
                        }
                );
    }

    // =========================================================
    // Public API — save / reload
    // =========================================================

    @Override
    @Transactional
    public WorkflowDefinition saveWorkflow(String workflowJson, String updatedBy) {
        // validate JSON is a valid WorkflowDefinition before saving
        WorkflowDefinition def = parse(workflowJson);

        long now = System.currentTimeMillis();
        WorkflowConfigEntity entity = workflowConfigRepo
                .findByBusinessServiceAndIsActiveTrue(BUSINESS_SERVICE)
                .orElse(WorkflowConfigEntity.builder()
                        .businessService(BUSINESS_SERVICE)
                        .createdAt(now)
                        .build());

        entity.setWorkflowJson(workflowJson);
        entity.setIsActive(true);
        entity.setUpdatedAt(now);
        entity.setUpdatedBy(updatedBy);
        workflowConfigRepo.save(entity);

        // hot-reload in memory
        applyDefinition(def);
        log.info("Workflow updated and reloaded by '{}'", updatedBy);
        return def;
    }

    @Override
    public WorkflowDefinition getWorkflow() {
        String json = workflowConfigRepo.findByBusinessServiceAndIsActiveTrue(BUSINESS_SERVICE)
                .map(WorkflowConfigEntity::getWorkflowJson)
                .orElseGet(this::loadFromFile);
        return parse(json);
    }

    // =========================================================
    // Transition validation
    // =========================================================

    @Override
    public WorkflowActionDef validateAndGetAction(String currentState, String action, List<String> userRoles) {
        String stateKey = currentState == null ? "__START__" : currentState;
        WorkflowStateDef stateDef = stateMap.get(stateKey);

        if (stateDef == null)
            throw new IllegalArgumentException("Unknown workflow state: " + currentState);

        if (stateDef.isTerminateState())
            throw new IllegalArgumentException(
                    "Ticket is in terminal state '" + currentState + "'. No further actions allowed.");

        return stateDef.getActions().stream()
                .filter(a -> a.getAction().equals(action))
                .findFirst()
                .map(a -> {
                    boolean hasRole = userRoles.stream().anyMatch(r -> a.getRoles().contains(r));
                    if (!hasRole)
                        throw new IllegalArgumentException(
                                "Action '" + action + "' not allowed for your role. Required: " + a.getRoles());
                    return a;
                })
                .orElseThrow(() -> new IllegalArgumentException(
                        "Action '" + action + "' is not valid in state '" + currentState + "'. Valid actions: "
                        + stateDef.getActions().stream().map(WorkflowActionDef::getAction).toList()));
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

    // =========================================================
    // Internals
    // =========================================================

    private void parseAndApply(String json) {
        applyDefinition(parse(json));
    }

    private void applyDefinition(WorkflowDefinition def) {
        stateMap = def.getStates().stream()
                .collect(Collectors.toMap(
                        s -> s.getState() == null ? "__START__" : s.getState(),
                        Function.identity()
                ));
    }

    private WorkflowDefinition parse(String json) {
        try {
            return objectMapper.readValue(json, WorkflowDefinition.class);
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid workflow JSON: " + e.getMessage(), e);
        }
    }

    private String loadFromFile() {
        try {
            ClassPathResource resource = new ClassPathResource("pmis_ticket_workflow.json");
            return new String(resource.getInputStream().readAllBytes());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load pmis_ticket_workflow.json from classpath", e);
        }
    }

    private void saveToDb(String json, String updatedBy) {
        long now = System.currentTimeMillis();
        workflowConfigRepo.save(WorkflowConfigEntity.builder()
                .businessService(BUSINESS_SERVICE)
                .workflowJson(json)
                .isActive(true)
                .createdAt(now)
                .updatedAt(now)
                .updatedBy(updatedBy)
                .build());
    }
}
