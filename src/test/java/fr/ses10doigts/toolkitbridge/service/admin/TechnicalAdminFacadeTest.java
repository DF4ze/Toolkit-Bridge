package fr.ses10doigts.toolkitbridge.service.admin;

import fr.ses10doigts.toolkitbridge.model.dto.admin.technical.TechnicalAdminView;
import fr.ses10doigts.toolkitbridge.service.admin.config.AdminTechnicalProperties;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskStatus;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEventType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TechnicalAdminFacadeTest {

    @Test
    void configurationViewDelegatesToAdminConfigQueryService() {
        AdminConfigQueryService adminConfigQueryService = mock(AdminConfigQueryService.class);
        TechnicalAdminView.ConfigItem expected = new TechnicalAdminView.ConfigItem(1, 1, List.of(
                new TechnicalAdminView.LlmProviderItem("openai", "https://api.openai.com/v1", "gpt-4.1-mini", true)
        ));
        when(adminConfigQueryService.getConfigurationView()).thenReturn(expected);

        TechnicalAdminFacade facade = facadeWith(
                mock(AdminAgentQueryService.class),
                mock(AdminTaskQueryService.class),
                mock(TraceQueryService.class),
                mock(ArtifactQueryService.class),
                adminConfigQueryService,
                mock(RetentionQueryService.class),
                new AdminTechnicalProperties()
        );

        TechnicalAdminView.ConfigItem config = facade.getConfigurationView();

        assertThat(config).isEqualTo(expected);
        verify(adminConfigQueryService).getConfigurationView();
    }

    @Test
    void listRetentionPoliciesDelegatesToRetentionQueryService() {
        RetentionQueryService retentionQueryService = mock(RetentionQueryService.class);
        List<TechnicalAdminView.RetentionItem> expected = List.of(
                new TechnicalAdminView.RetentionItem("TASK", java.time.Duration.ofDays(30), "PRESERVE")
        );
        when(retentionQueryService.listRetentionPolicies()).thenReturn(expected);

        TechnicalAdminFacade facade = facadeWith(
                mock(AdminAgentQueryService.class),
                mock(AdminTaskQueryService.class),
                mock(TraceQueryService.class),
                mock(ArtifactQueryService.class),
                mock(AdminConfigQueryService.class),
                retentionQueryService,
                new AdminTechnicalProperties()
        );

        List<TechnicalAdminView.RetentionItem> items = facade.listRetentionPolicies();

        assertThat(items).isEqualTo(expected);
        verify(retentionQueryService).listRetentionPolicies();
    }

    @Test
    void listAgentsDelegatesToAdminAgentQueryService() {
        AdminAgentQueryService adminAgentQueryService = mock(AdminAgentQueryService.class);
        List<TechnicalAdminView.AgentItem> expected = List.of(new TechnicalAdminView.AgentItem(
                "agent-1",
                "Agent One",
                "ASSISTANT",
                "CHAT",
                "openai",
                "gpt-4.1-mini",
                "default",
                true,
                null,
                null,
                List.of("read_file")
        ));
        when(adminAgentQueryService.listAgents()).thenReturn(expected);

        TechnicalAdminFacade facade = facadeWith(
                adminAgentQueryService,
                mock(AdminTaskQueryService.class),
                mock(TraceQueryService.class),
                mock(ArtifactQueryService.class),
                mock(AdminConfigQueryService.class),
                mock(RetentionQueryService.class),
                new AdminTechnicalProperties()
        );

        List<TechnicalAdminView.AgentItem> items = facade.listAgents();

        assertThat(items).isEqualTo(expected);
        verify(adminAgentQueryService).listAgents();
    }

    @Test
    void listRecentTasksDelegatesToAdminTaskQueryService() {
        AdminTaskQueryService adminTaskQueryService = mock(AdminTaskQueryService.class);
        List<TechnicalAdminView.TaskItem> expected = List.of(taskItem("task-1", TaskStatus.RUNNING));
        when(adminTaskQueryService.listRecentTasks(12, "agent-1", TaskStatus.RUNNING)).thenReturn(expected);

        TechnicalAdminFacade facade = facadeWith(
                mock(AdminAgentQueryService.class),
                adminTaskQueryService,
                mock(TraceQueryService.class),
                mock(ArtifactQueryService.class),
                mock(AdminConfigQueryService.class),
                mock(RetentionQueryService.class),
                new AdminTechnicalProperties()
        );

        List<TechnicalAdminView.TaskItem> items = facade.listRecentTasks(12, "agent-1", TaskStatus.RUNNING);

        assertThat(items).isEqualTo(expected);
        verify(adminTaskQueryService).listRecentTasks(12, "agent-1", TaskStatus.RUNNING);
    }

    @Test
    void listRecentTracesDelegatesToTraceQueryService() {
        TraceQueryService traceQueryService = mock(TraceQueryService.class);
        List<TechnicalAdminView.TraceItem> expected = List.of(traceItem(AgentTraceEventType.RESPONSE));
        when(traceQueryService.listRecentTraces(20, "agent-1")).thenReturn(expected);

        TechnicalAdminFacade facade = facadeWith(
                mock(AdminAgentQueryService.class),
                mock(AdminTaskQueryService.class),
                traceQueryService,
                mock(ArtifactQueryService.class),
                mock(AdminConfigQueryService.class),
                mock(RetentionQueryService.class),
                new AdminTechnicalProperties()
        );

        List<TechnicalAdminView.TraceItem> items = facade.listRecentTraces(20, "agent-1");

        assertThat(items).isEqualTo(expected);
        verify(traceQueryService).listRecentTraces(20, "agent-1");
    }

    @Test
    void listRecentArtifactsDelegatesToArtifactQueryService() {
        ArtifactQueryService artifactQueryService = mock(ArtifactQueryService.class);
        List<TechnicalAdminView.ArtifactItem> expected = List.of(artifactItem("artifact-1"));
        when(artifactQueryService.listRecentArtifacts(30, "agent-1", "task-1")).thenReturn(expected);

        TechnicalAdminFacade facade = facadeWith(
                mock(AdminAgentQueryService.class),
                mock(AdminTaskQueryService.class),
                mock(TraceQueryService.class),
                artifactQueryService,
                mock(AdminConfigQueryService.class),
                mock(RetentionQueryService.class),
                new AdminTechnicalProperties()
        );

        List<TechnicalAdminView.ArtifactItem> items = facade.listRecentArtifacts(30, "agent-1", "task-1");

        assertThat(items).isEqualTo(expected);
        verify(artifactQueryService).listRecentArtifacts(30, "agent-1", "task-1");
    }

    @Test
    void getOverviewAggregatesListsAndCountersWithSanitizedLimit() {
        AdminAgentQueryService adminAgentQueryService = mock(AdminAgentQueryService.class);
        AdminTaskQueryService adminTaskQueryService = mock(AdminTaskQueryService.class);
        TraceQueryService traceQueryService = mock(TraceQueryService.class);
        ArtifactQueryService artifactQueryService = mock(ArtifactQueryService.class);
        AdminConfigQueryService adminConfigQueryService = mock(AdminConfigQueryService.class);
        RetentionQueryService retentionQueryService = mock(RetentionQueryService.class);
        AdminTechnicalProperties properties = new AdminTechnicalProperties();
        properties.setDefaultListLimit(5);
        properties.setMaxListLimit(2);

        when(adminAgentQueryService.listAgents()).thenReturn(List.of(
                agent("agent-1", true),
                agent("agent-2", false)
        ));

        when(adminTaskQueryService.listRecentTasks(2, null, null)).thenReturn(List.of(
                taskItem("task-1", TaskStatus.RUNNING),
                taskItem("task-2", TaskStatus.DONE)
        ));
        when(traceQueryService.listRecentTraces(2, null)).thenReturn(List.of(
                traceItem(AgentTraceEventType.ERROR),
                traceItem(AgentTraceEventType.RESPONSE)
        ));
        when(artifactQueryService.listRecentArtifacts(2, null, null)).thenReturn(List.of(
                artifactItem("artifact-1"),
                artifactItem("artifact-2")
        ));

        when(adminConfigQueryService.getConfigurationView()).thenReturn(new TechnicalAdminView.ConfigItem(
                2,
                1,
                List.of(new TechnicalAdminView.LlmProviderItem(
                        "openai",
                        "https://api.openai.com/v1",
                        "gpt-4.1-mini",
                        true
                ))
        ));
        when(retentionQueryService.listRetentionPolicies()).thenReturn(List.of(
                new TechnicalAdminView.RetentionItem("TASK", java.time.Duration.ofDays(30), "PRESERVE")
        ));

        TechnicalAdminFacade facade = facadeWith(
                adminAgentQueryService,
                adminTaskQueryService,
                traceQueryService,
                artifactQueryService,
                adminConfigQueryService,
                retentionQueryService,
                properties
        );

        TechnicalAdminView.Overview overview = facade.getOverview(999);

        assertThat(overview.agents()).isEqualTo(2);
        assertThat(overview.busyAgents()).isEqualTo(1);
        assertThat(overview.recentTasks()).isEqualTo(2);
        assertThat(overview.recentTraces()).isEqualTo(2);
        assertThat(overview.recentErrors()).isEqualTo(1);
        assertThat(overview.recentArtifacts()).isEqualTo(2);
        assertThat(overview.tasks()).hasSize(2);
        assertThat(overview.traces()).hasSize(2);
        assertThat(overview.artifacts()).hasSize(2);

        verify(adminAgentQueryService).listAgents();
        verify(adminTaskQueryService).listRecentTasks(2, null, null);
        verify(traceQueryService).listRecentTraces(2, null);
        verify(artifactQueryService).listRecentArtifacts(2, null, null);
        verify(adminConfigQueryService).getConfigurationView();
        verify(retentionQueryService).listRetentionPolicies();
    }

    @Test
    void getOverviewCountsOnlyErrorTraceTypeForRecentErrors() {
        AdminAgentQueryService adminAgentQueryService = mock(AdminAgentQueryService.class);
        TraceQueryService traceQueryService = mock(TraceQueryService.class);
        when(adminAgentQueryService.listAgents()).thenReturn(List.of(agent("agent-1", false)));
        when(traceQueryService.listRecentTraces(50, null)).thenReturn(List.of(
                traceItem(AgentTraceEventType.ERROR),
                traceItem(AgentTraceEventType.RESPONSE),
                traceItem(null)
        ));

        AdminTaskQueryService adminTaskQueryService = mock(AdminTaskQueryService.class);
        when(adminTaskQueryService.listRecentTasks(50, null, null)).thenReturn(List.of());
        ArtifactQueryService artifactQueryService = mock(ArtifactQueryService.class);
        when(artifactQueryService.listRecentArtifacts(50, null, null)).thenReturn(List.of());

        AdminConfigQueryService adminConfigQueryService = mock(AdminConfigQueryService.class);
        when(adminConfigQueryService.getConfigurationView()).thenReturn(new TechnicalAdminView.ConfigItem(1, 0, List.of()));
        RetentionQueryService retentionQueryService = mock(RetentionQueryService.class);
        when(retentionQueryService.listRetentionPolicies()).thenReturn(List.of());

        TechnicalAdminFacade facade = facadeWith(
                adminAgentQueryService,
                adminTaskQueryService,
                traceQueryService,
                artifactQueryService,
                adminConfigQueryService,
                retentionQueryService,
                new AdminTechnicalProperties()
        );

        TechnicalAdminView.Overview overview = facade.getOverview(null);

        assertThat(overview.recentTraces()).isEqualTo(3);
        assertThat(overview.recentErrors()).isEqualTo(1);
    }

    private TechnicalAdminFacade facadeWith(
            AdminAgentQueryService adminAgentQueryService,
            AdminTaskQueryService adminTaskQueryService,
            TraceQueryService traceQueryService,
            ArtifactQueryService artifactQueryService,
            AdminConfigQueryService adminConfigQueryService,
            RetentionQueryService retentionQueryService,
            AdminTechnicalProperties properties
    ) {
        return new TechnicalAdminFacade(
                adminAgentQueryService,
                adminTaskQueryService,
                traceQueryService,
                artifactQueryService,
                adminConfigQueryService,
                retentionQueryService,
                properties
        );
    }

    private TechnicalAdminView.AgentItem agent(String id, boolean busy) {
        return new TechnicalAdminView.AgentItem(
                id,
                "Agent " + id,
                "ASSISTANT",
                "CHAT",
                "openai",
                "gpt-4.1-mini",
                "default",
                true,
                new TechnicalAdminView.RuntimeItem(
                        "AVAILABLE",
                        busy,
                        busy ? "task" : null,
                        null,
                        null,
                        null,
                        null,
                        Instant.parse("2026-01-01T10:00:00Z"),
                        Instant.parse("2026-01-01T10:00:30Z")
                ),
                null,
                List.of()
        );
    }

    private TechnicalAdminView.TaskItem taskItem(String id, TaskStatus status) {
        return new TechnicalAdminView.TaskItem(
                id,
                null,
                "objective",
                "user",
                "agent-1",
                "trace-" + id,
                "TASK_ORCHESTRATOR",
                status,
                "telegram",
                "conv",
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-01T10:01:00Z"),
                null,
                0
        );
    }

    private TechnicalAdminView.TraceItem traceItem(AgentTraceEventType type) {
        return new TechnicalAdminView.TraceItem(
                Instant.parse("2026-01-01T10:00:00Z"),
                type,
                "source",
                "run",
                "agent-1",
                "msg",
                "task",
                java.util.Map.of("k", "v")
        );
    }

    private TechnicalAdminView.ArtifactItem artifactItem(String id) {
        return new TechnicalAdminView.ArtifactItem(
                id,
                "REPORT",
                "task-1",
                "agent-1",
                "title",
                Instant.parse("2026-01-01T10:00:00Z"),
                Instant.parse("2026-01-02T10:00:00Z"),
                "workspace",
                "artifacts/" + id + ".md",
                "text/markdown",
                10
        );
    }
}
