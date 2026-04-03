package fr.ses10doigts.toolkitbridge.service.agent.artifact.service;

import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.Artifact;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactContentPointer;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactDraft;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactType;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.port.ArtifactContentStore;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.port.ArtifactMetadataStore;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArtifactServiceTest {

    @Test
    void createsArtifactWithStoredContentAndRetention() {
        ArtifactMetadataStore metadataStore = mock(ArtifactMetadataStore.class);
        ArtifactContentStore contentStore = mock(ArtifactContentStore.class);
        ArtifactRetentionPolicy retentionPolicy = mock(ArtifactRetentionPolicy.class);

        when(contentStore.store(any(), eq(ArtifactType.PLAN), eq("step1"), eq("text/markdown"), eq("plan.md")))
                .thenReturn(new ArtifactContentPointer("workspace", "artifacts/plan/123/plan.md", "text/markdown", 5));
        when(retentionPolicy.computeExpiration(eq(ArtifactType.PLAN), any()))
                .thenReturn(Instant.parse("2026-01-10T00:00:00Z"));
        when(metadataStore.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ArtifactService service = new ArtifactService(metadataStore, contentStore, retentionPolicy);
        Artifact artifact = service.create(new ArtifactDraft(
                ArtifactType.PLAN,
                "task-42",
                "agent-5",
                "Execution plan",
                Map.of("origin", "orchestrator"),
                "step1",
                "text/markdown",
                "plan.md"
        ));

        assertThat(artifact.taskId()).isEqualTo("task-42");
        assertThat(artifact.type()).isEqualTo(ArtifactType.PLAN);
        assertThat(artifact.contentPointer().location()).contains("artifacts/plan");
        assertThat(artifact.expiresAt()).isEqualTo(Instant.parse("2026-01-10T00:00:00Z"));
    }
}
