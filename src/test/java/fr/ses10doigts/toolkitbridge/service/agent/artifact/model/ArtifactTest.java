package fr.ses10doigts.toolkitbridge.service.agent.artifact.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArtifactTest {

    @Test
    void keepsMetadataImmutableAndBuildsReference() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("origin", "task_orchestrator");

        Artifact artifact = new Artifact(
                "artifact-1",
                ArtifactType.SUMMARY,
                "task-1",
                "agent-1",
                "Conversation summary",
                Instant.parse("2026-01-01T00:00:00Z"),
                null,
                metadata,
                new ArtifactContentPointer("workspace", "artifacts/summary/a1.txt", "text/plain", 12)
        );

        metadata.put("origin", "other");

        assertThat(artifact.metadata()).containsEntry("origin", "task_orchestrator");
        assertThatThrownBy(() -> artifact.metadata().put("new", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(artifact.toReference().artifactId()).isEqualTo("artifact-1");
        assertThat(artifact.toReference().type()).isEqualTo(ArtifactType.SUMMARY);
    }
}
