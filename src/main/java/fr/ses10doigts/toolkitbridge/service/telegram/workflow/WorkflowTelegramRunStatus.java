package fr.ses10doigts.toolkitbridge.service.telegram.workflow;

public enum WorkflowTelegramRunStatus {
    IDLE,
    RUNNING,
    COMPLETED,
    WAITING_HUMAN,
    FAILED
}

