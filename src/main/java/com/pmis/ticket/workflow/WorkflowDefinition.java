package com.pmis.ticket.workflow;

import lombok.Data;
import java.util.List;

@Data
public class WorkflowDefinition {
    private String                  businessService;
    private List<WorkflowStateDef>  states;
}
