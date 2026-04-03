package fr.ses10doigts.toolkitbridge.service.agent.artifact.model;

import java.util.Objects;

public record ArtifactReference(
        String artifactId,
        ArtifactType type
) {

    public ArtifactReference {
        if (artifactId == null || artifactId.isBlank()) {
            throw new IllegalArgumentException("artifactId must not be blank");
        }
        Objects.requireNonNull(type, "type must not be null");
    }
}
