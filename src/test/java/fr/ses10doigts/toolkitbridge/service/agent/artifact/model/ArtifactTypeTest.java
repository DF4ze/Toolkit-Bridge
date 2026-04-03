package fr.ses10doigts.toolkitbridge.service.agent.artifact.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArtifactTypeTest {

    @Test
    void resolvesTypeFromLabelAndKey() {
        assertThat(ArtifactType.fromLabel("report")).isEqualTo(ArtifactType.REPORT);
        assertThat(ArtifactType.fromLabel("REPORT")).isEqualTo(ArtifactType.REPORT);
        assertThat(ArtifactType.fromLabel("memory-candidate")).isEqualTo(ArtifactType.MEMORY_CANDIDATE);
        assertThat(ArtifactType.fromLabel("memory candidate")).isEqualTo(ArtifactType.MEMORY_CANDIDATE);
        assertThat(ArtifactType.fromLabel("MEMORY_CANDIDATE")).isEqualTo(ArtifactType.MEMORY_CANDIDATE);
    }
}
