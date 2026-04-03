package fr.ses10doigts.toolkitbridge.service.agent.artifact.model;

import java.util.Arrays;
import java.util.Locale;

public enum ArtifactType {
    REPORT("report"),
    PATCH("patch"),
    SCRIPT("script"),
    PLAN("plan"),
    SUMMARY("summary"),
    FILE("file"),
    MEMORY_CANDIDATE("memory candidate");

    private final String label;

    ArtifactType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static ArtifactType fromLabel(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("artifact type must not be blank");
        }
        String normalized = normalize(value);
        return Arrays.stream(values())
                .filter(type -> normalize(type.name()).equals(normalized)
                        || normalize(type.key()).equals(normalized)
                        || normalize(type.label()).equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown artifact type: " + value));
    }

    private static String normalize(String value) {
        return value.trim()
                .toLowerCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }
}
