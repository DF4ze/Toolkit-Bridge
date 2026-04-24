package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.codex;

import java.nio.file.Path;

public record CodexExecutionRequest(
        String prompt,
        Path workingDirectory,
        Integer timeoutSeconds
) {

    public CodexExecutionRequest {
        if (prompt == null || prompt.isBlank()) {
            throw new IllegalArgumentException("prompt must not be blank");
        }
        if (timeoutSeconds != null && timeoutSeconds <= 0) {
            throw new IllegalArgumentException("timeoutSeconds must be greater than 0");
        }

        prompt = prompt.trim();
    }
}
