package fr.ses10doigts.toolkitbridge.service.agent.artifact.service;

import fr.ses10doigts.toolkitbridge.service.agent.artifact.config.ArtifactStorageProperties;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactType;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ArtifactRetentionPolicyTest {

    @Test
    void computesExpirationFromTypeSpecificConfiguration() {
        ArtifactStorageProperties properties = new ArtifactStorageProperties();
        properties.getRetention().setDefaultTtl(Duration.ofHours(10));
        properties.getRetention().setByType(Map.of(
                "memory-candidate", Duration.ofHours(24),
                ArtifactType.REPORT.key(), Duration.ofHours(48)
        ));

        ArtifactRetentionPolicy policy = new ArtifactRetentionPolicy(properties);
        Instant now = Instant.parse("2026-01-01T10:00:00Z");

        assertThat(policy.computeExpiration(ArtifactType.REPORT, now))
                .isEqualTo(Instant.parse("2026-01-03T10:00:00Z"));
        assertThat(policy.computeExpiration(ArtifactType.SCRIPT, now))
                .isEqualTo(Instant.parse("2026-01-01T20:00:00Z"));
        assertThat(policy.computeExpiration(ArtifactType.MEMORY_CANDIDATE, now))
                .isEqualTo(Instant.parse("2026-01-02T10:00:00Z"));
    }
}
