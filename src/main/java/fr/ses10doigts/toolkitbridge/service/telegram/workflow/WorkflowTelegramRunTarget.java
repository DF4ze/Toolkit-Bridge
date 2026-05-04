package fr.ses10doigts.toolkitbridge.service.telegram.workflow;

public record WorkflowTelegramRunTarget(
        String projectName,
        Integer phase,
        Integer etape,
        boolean valid,
        String errorMessage,
        WorkflowTelegramRunTargetErrorCode errorCode
) {

    public static WorkflowTelegramRunTarget ok(String projectName, Integer phase, Integer etape) {
        return new WorkflowTelegramRunTarget(projectName, phase, etape, true, null, null);
    }

    public static WorkflowTelegramRunTarget error(String message) {
        return error(message, null);
    }

    public static WorkflowTelegramRunTarget error(String message, WorkflowTelegramRunTargetErrorCode errorCode) {
        return new WorkflowTelegramRunTarget(null, null, null, false, message == null ? "" : message.trim(), errorCode);
    }
}
