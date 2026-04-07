package fr.ses10doigts.toolkitbridge.service.agent.improvement.model;

import java.util.Map;

public record ImprovementObservation(
        String category,
        String title,
        String detail,
        Map<String, String> evidence
) {
    public ImprovementObservation {
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("category must not be blank");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        if (detail == null || detail.isBlank()) {
            throw new IllegalArgumentException("detail must not be blank");
        }
        evidence = evidence == null ? Map.of() : Map.copyOf(evidence);
    }
}
