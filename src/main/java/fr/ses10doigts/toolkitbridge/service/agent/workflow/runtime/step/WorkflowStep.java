package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step;

import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowExecutionContext;

@FunctionalInterface
public interface WorkflowStep {
    WorkflowStepResult execute(WorkflowExecutionContext context);
}
