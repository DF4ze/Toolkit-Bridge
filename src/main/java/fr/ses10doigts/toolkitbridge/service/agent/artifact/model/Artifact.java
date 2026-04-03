package fr.ses10doigts.toolkitbridge.service.agent.artifact.model;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

public record Artifact(
        String artifactId,
        ArtifactType type,
        String taskId,
        String producerAgentId,
        String title,
        Instant createdAt,
        Instant expiresAt,
        Map<String, String> metadata,
        ArtifactContentPointer contentPointer
) {

    public Artifact {
        if (artifactId == null || artifactId.isBlank()) {
            throw new IllegalArgumentException("artifactId must not be blank");
        }
        Objects.requireNonNull(type, "type must not be null");
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("taskId must not be blank");
        }
        if (producerAgentId == null || producerAgentId.isBlank()) {
            throw new IllegalArgumentException("producerAgentId must not be blank");
        }
        if (title != null && title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank when provided");
        }
        createdAt = createdAt == null ? Instant.now() : createdAt;
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        Objects.requireNonNull(contentPointer, "contentPointer must not be null");
    }

    public ArtifactReference toReference() {
        return new ArtifactReference(artifactId, type);
    }
}
