package fr.ses10doigts.toolkitbridge.service.admin;

import fr.ses10doigts.toolkitbridge.model.dto.admin.technical.TechnicalAdminView;
import fr.ses10doigts.toolkitbridge.service.admin.config.AdminTechnicalProperties;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.Artifact;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactContentPointer;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactType;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.service.ArtifactService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ArtifactQueryServiceTest {

    @Test
    void listRecentArtifactsPrioritizesTaskFilterAndAppliesSortingAndLimit() {
        ArtifactService artifactService = mock(ArtifactService.class);
        AdminTechnicalProperties properties = new AdminTechnicalProperties();
        properties.setMaxListLimit(2);

        when(artifactService.findByTaskId("task-1")).thenReturn(List.of(
                artifact("artifact-old", "task-1", "agent-a", Instant.parse("2026-01-01T08:00:00Z")),
                artifact("artifact-new", "task-1", "agent-a", Instant.parse("2026-01-01T10:00:00Z")),
                artifact("artifact-mid", "task-1", "agent-a", Instant.parse("2026-01-01T09:00:00Z"))
        ));

        ArtifactQueryService service = new ArtifactQueryService(artifactService, properties);

        List<TechnicalAdminView.ArtifactItem> items = service.listRecentArtifacts(999, "agent-a", "task-1");

        assertThat(items).extracting(TechnicalAdminView.ArtifactItem::artifactId)
                .containsExactly("artifact-new", "artifact-mid");
        verify(artifactService).findByTaskId("task-1");
        verify(artifactService, never()).findByProducerAgentId(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt());
        verify(artifactService, never()).findRecent(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void listRecentArtifactsUsesAgentFilterWhenTaskIdIsBlank() {
        ArtifactService artifactService = mock(ArtifactService.class);
        AdminTechnicalProperties properties = new AdminTechnicalProperties();

        when(artifactService.findByProducerAgentId("agent-9", 3)).thenReturn(List.of(
                artifact("artifact-1", "task-a", "agent-9", Instant.parse("2026-01-01T10:00:00Z"))
        ));

        ArtifactQueryService service = new ArtifactQueryService(artifactService, properties);

        List<TechnicalAdminView.ArtifactItem> items = service.listRecentArtifacts(3, "agent-9", "   ");

        assertThat(items).hasSize(1);
        assertThat(items.get(0).producerAgentId()).isEqualTo("agent-9");
        verify(artifactService).findByProducerAgentId("agent-9", 3);
        verify(artifactService, never()).findByTaskId(org.mockito.ArgumentMatchers.anyString());
        verify(artifactService, never()).findRecent(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void listRecentArtifactsFallsBackToGlobalWhenNoFilters() {
        ArtifactService artifactService = mock(ArtifactService.class);
        AdminTechnicalProperties properties = new AdminTechnicalProperties();
        properties.setDefaultListLimit(4);
        properties.setMaxListLimit(4);

        when(artifactService.findRecent(4)).thenReturn(List.of(
                artifact("artifact-2", "task-a", "agent-a", Instant.parse("2026-01-01T09:00:00Z")),
                artifact("artifact-3", "task-b", "agent-b", Instant.parse("2026-01-01T08:00:00Z")),
                artifact("artifact-1", "task-c", "agent-c", Instant.parse("2026-01-01T10:00:00Z"))
        ));

        ArtifactQueryService service = new ArtifactQueryService(artifactService, properties);

        List<TechnicalAdminView.ArtifactItem> items = service.listRecentArtifacts(null, "  ", null);

        assertThat(items).extracting(TechnicalAdminView.ArtifactItem::artifactId)
                .containsExactly("artifact-1", "artifact-2", "artifact-3");
        verify(artifactService).findRecent(4);
        verify(artifactService, never()).findByTaskId(org.mockito.ArgumentMatchers.anyString());
        verify(artifactService, never()).findByProducerAgentId(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyInt());
    }

    private Artifact artifact(String id, String taskId, String agentId, Instant createdAt) {
        return new Artifact(
                id,
                ArtifactType.REPORT,
                taskId,
                agentId,
                "title-" + id,
                createdAt,
                createdAt.plus(java.time.Duration.ofDays(1)),
                Map.of(),
                new ArtifactContentPointer("workspace", "artifacts/" + id + ".md", "text/markdown", 10)
        );
    }
}

