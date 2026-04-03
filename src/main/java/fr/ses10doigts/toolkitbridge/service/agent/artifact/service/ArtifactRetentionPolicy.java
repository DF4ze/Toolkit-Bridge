package fr.ses10doigts.toolkitbridge.service.agent.artifact.service;

import fr.ses10doigts.toolkitbridge.service.agent.artifact.config.ArtifactStorageProperties;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactType;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ArtifactRetentionPolicy {

    private final ArtifactStorageProperties properties;

    public ArtifactRetentionPolicy(ArtifactStorageProperties properties) {
        this.properties = properties;
    }

    public Instant computeExpiration(ArtifactType type, Instant createdAt) {
        Instant safeCreatedAt = createdAt == null ? Instant.now() : createdAt;
        return safeCreatedAt.plus(properties.getRetention().ttlFor(type));
    }
}
