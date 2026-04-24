package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model;

import java.util.Map;
import java.util.Objects;

public record WorkflowExecutionContext(
        WorkflowRun workflowRun,
        LotRun lotRun,
        Map<String, Object> variables
) {

    public WorkflowExecutionContext {
        Objects.requireNonNull(workflowRun, "workflowRun must not be null");
        variables = variables == null ? Map.of() : Map.copyOf(variables);
    }
}
