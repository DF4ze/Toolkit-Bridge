package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step;

import java.util.Map;
import java.util.Objects;

public record WorkflowStepResult(
        WorkflowStepDecision decision,
        String message,
        Map<String, Object> data
) {

    public WorkflowStepResult {
        Objects.requireNonNull(decision, "decision must not be null");
        message = normalize(message);
        data = data == null ? Map.of() : Map.copyOf(data);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
