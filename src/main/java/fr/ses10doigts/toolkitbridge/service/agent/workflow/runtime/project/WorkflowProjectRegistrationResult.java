package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.project;

import java.nio.file.Path;

public record WorkflowProjectRegistrationResult(
        boolean success,
        boolean created,
        String projectKey,
        String projectName,
        Path normalizedProjectPath,
        String reason
) {

    public static WorkflowProjectRegistrationResult created(String projectKey, String projectName, Path normalizedProjectPath) {
        return new WorkflowProjectRegistrationResult(true, true, projectKey, projectName, normalizedProjectPath, null);
    }

    public static WorkflowProjectRegistrationResult updated(String projectKey, String projectName, Path normalizedProjectPath) {
        return new WorkflowProjectRegistrationResult(true, false, projectKey, projectName, normalizedProjectPath, null);
    }

    public static WorkflowProjectRegistrationResult failed(String reason) {
        return new WorkflowProjectRegistrationResult(false, false, null, null, null, reason);
    }
}

