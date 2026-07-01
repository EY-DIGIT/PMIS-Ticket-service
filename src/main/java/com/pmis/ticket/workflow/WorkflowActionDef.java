package com.pmis.ticket.workflow;

import lombok.Data;
import java.util.List;

@Data
public class WorkflowActionDef {
    private String       action;
    private String       nextState;
    private List<String> roles;
}
