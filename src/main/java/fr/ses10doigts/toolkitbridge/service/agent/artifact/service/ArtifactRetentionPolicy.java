package fr.ses10doigts.toolkitbridge.service.agent.artifact.service;

import fr.ses10doigts.toolkitbridge.persistence.model.PersistableObjectFamily;
import fr.ses10doigts.toolkitbridge.persistence.retention.PersistenceRetentionPolicyResolver;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactType;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ArtifactRetentionPolicy {

    private final PersistenceRetentionPolicyResolver retentionPolicyResolver;

    public ArtifactRetentionPolicy(PersistenceRetentionPolicyResolver retentionPolicyResolver) {
        this.retentionPolicyResolver = retentionPolicyResolver;
    }

    public Instant computeExpiration(ArtifactType type, Instant createdAt) {
        return retentionPolicyResolver.computeExpiration(
                PersistableObjectFamily.ARTIFACT,
                type == null ? null : type.key(),
                createdAt
        );
    }
}
