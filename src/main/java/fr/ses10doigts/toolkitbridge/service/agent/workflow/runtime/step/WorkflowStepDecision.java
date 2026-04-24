package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step;

public enum WorkflowStepDecision {
    CONTINUE,
    RETRY_CORRECTION,
    WAIT_HUMAN,
    STOP_FAILURE,
    FINISH
}
