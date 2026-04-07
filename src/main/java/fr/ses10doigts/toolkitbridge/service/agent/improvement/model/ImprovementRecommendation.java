package fr.ses10doigts.toolkitbridge.service.agent.improvement.model;

public record ImprovementRecommendation(
        ImprovementTargetType targetType,
        String targetReference,
        String summary,
        String rationale,
        String suggestedChange
) {
    public ImprovementRecommendation {
        if (targetType == null) {
            throw new IllegalArgumentException("targetType must not be null");
        }
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("summary must not be blank");
        }
        if (rationale == null || rationale.isBlank()) {
            throw new IllegalArgumentException("rationale must not be blank");
        }
        if (suggestedChange == null || suggestedChange.isBlank()) {
            throw new IllegalArgumentException("suggestedChange must not be blank");
        }
        targetReference = normalize(targetReference);
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
