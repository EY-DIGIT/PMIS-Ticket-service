package com.pmis.ticket.workflow;

import lombok.Data;
import java.util.List;

@Data
public class WorkflowStateDef {
    private String               state;
    private boolean              isStartState;
    private boolean              isTerminateState;
    private List<WorkflowActionDef> actions;
}
