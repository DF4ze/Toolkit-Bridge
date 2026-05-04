package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.project;

import java.nio.file.Path;

public record WorkflowProjectLookupResult(
        boolean found,
        String projectKey,
        String projectName,
        Path normalizedProjectPath,
        String reason
) {

    public static WorkflowProjectLookupResult found(String projectKey, String projectName, Path normalizedProjectPath) {
        return new WorkflowProjectLookupResult(true, projectKey, projectName, normalizedProjectPath, null);
    }

    public static WorkflowProjectLookupResult notFound(String reason) {
        return new WorkflowProjectLookupResult(false, null, null, null, reason);
    }
}

