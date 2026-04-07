package fr.ses10doigts.toolkitbridge.service.agent.artifact.service;

import fr.ses10doigts.toolkitbridge.persistence.retention.PersistenceRetentionPolicyResolver;
import fr.ses10doigts.toolkitbridge.persistence.retention.PersistenceRetentionProperties;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactType;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class ArtifactRetentionPolicyTest {

    @Test
    void computesExpirationFromTypeSpecificConfiguration() {
        PersistenceRetentionProperties properties = new PersistenceRetentionProperties();
        PersistenceRetentionPolicyResolver resolver = new PersistenceRetentionPolicyResolver(properties);
        ArtifactRetentionPolicy policy = new ArtifactRetentionPolicy(resolver);
        Instant now = Instant.parse("2026-01-01T10:00:00Z");

        assertThat(policy.computeExpiration(ArtifactType.REPORT, now))
                .isEqualTo(Instant.parse("2026-01-08T10:00:00Z"));
        assertThat(policy.computeExpiration(ArtifactType.SCRIPT, now))
                .isEqualTo(Instant.parse("2026-01-08T10:00:00Z"));
        assertThat(policy.computeExpiration(ArtifactType.MEMORY_CANDIDATE, now))
                .isEqualTo(Instant.parse("2026-01-31T10:00:00Z"));
    }
}
