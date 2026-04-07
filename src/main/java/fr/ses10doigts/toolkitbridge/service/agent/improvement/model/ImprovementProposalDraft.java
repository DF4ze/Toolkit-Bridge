package fr.ses10doigts.toolkitbridge.service.agent.improvement.model;

import java.util.Map;

public record ImprovementProposalDraft(
        String taskId,
        String producerAgentId,
        String traceId,
        ImprovementObservation observation,
        ImprovementRecommendation recommendation,
        Map<String, String> metadata
) {
    public ImprovementProposalDraft {
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("taskId must not be blank");
        }
        if (producerAgentId == null || producerAgentId.isBlank()) {
            throw new IllegalArgumentException("producerAgentId must not be blank");
        }
        if (traceId == null || traceId.isBlank()) {
            throw new IllegalArgumentException("traceId must not be blank");
        }
        if (observation == null) {
            throw new IllegalArgumentException("observation must not be null");
        }
        if (recommendation == null) {
            throw new IllegalArgumentException("recommendation must not be null");
        }
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
