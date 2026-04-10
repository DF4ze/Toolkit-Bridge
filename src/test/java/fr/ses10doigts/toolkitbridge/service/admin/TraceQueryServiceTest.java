package fr.ses10doigts.toolkitbridge.service.admin;

import fr.ses10doigts.toolkitbridge.model.dto.admin.technical.TechnicalAdminView;
import fr.ses10doigts.toolkitbridge.service.admin.config.AdminTechnicalProperties;
import fr.ses10doigts.toolkitbridge.service.agent.trace.AgentTraceQueryService;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceCorrelation;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEvent;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEventType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TraceQueryServiceTest {

    @Test
    void listRecentTracesUsesGlobalEventsWhenAgentIdIsBlankAndSortsWithLimit() {
        AgentTraceQueryService agentTraceQueryService = mock(AgentTraceQueryService.class);
        AdminTechnicalProperties properties = new AdminTechnicalProperties();
        properties.setMaxListLimit(2);

        when(agentTraceQueryService.recentEvents()).thenReturn(List.of(
                trace(Instant.parse("2026-01-01T08:00:00Z"), AgentTraceEventType.TOOL_CALL, "agent-1"),
                trace(Instant.parse("2026-01-01T10:00:00Z"), AgentTraceEventType.ERROR, "agent-1"),
                trace(Instant.parse("2026-01-01T09:00:00Z"), AgentTraceEventType.RESPONSE, "agent-2")
        ));

        TraceQueryService service = new TraceQueryService(agentTraceQueryService, properties);

        List<TechnicalAdminView.TraceItem> items = service.listRecentTraces(999, "   ");

        assertThat(items).hasSize(2);
        assertThat(items.get(0).occurredAt()).isEqualTo(Instant.parse("2026-01-01T10:00:00Z"));
        assertThat(items.get(0).type()).isEqualTo(AgentTraceEventType.ERROR);
        assertThat(items.get(1).occurredAt()).isEqualTo(Instant.parse("2026-01-01T09:00:00Z"));
        verify(agentTraceQueryService).recentEvents();
        verify(agentTraceQueryService, never()).recentEventsForAgent(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void listRecentTracesUsesAgentSpecificEventsWhenAgentIdIsProvided() {
        AgentTraceQueryService agentTraceQueryService = mock(AgentTraceQueryService.class);
        AdminTechnicalProperties properties = new AdminTechnicalProperties();

        when(agentTraceQueryService.recentEventsForAgent("agent-9")).thenReturn(List.of(
                trace(Instant.parse("2026-01-01T10:00:00Z"), AgentTraceEventType.RESPONSE, "agent-9"),
                new AgentTraceEvent(
                        Instant.parse("2026-01-01T09:30:00Z"),
                        AgentTraceEventType.TOOL_CALL,
                        "source",
                        null,
                        Map.of("kind", "tool")
                )
        ));

        TraceQueryService service = new TraceQueryService(agentTraceQueryService, properties);

        List<TechnicalAdminView.TraceItem> items = service.listRecentTraces(10, "agent-9");

        assertThat(items).hasSize(2);
        assertThat(items.get(0).agentId()).isEqualTo("agent-9");
        assertThat(items.get(1).runId()).isNull();
        assertThat(items.get(1).taskId()).isNull();
        verify(agentTraceQueryService).recentEventsForAgent("agent-9");
        verify(agentTraceQueryService, never()).recentEvents();
    }

    private AgentTraceEvent trace(Instant at, AgentTraceEventType type, String agentId) {
        return new AgentTraceEvent(
                at,
                type,
                "source",
                new AgentTraceCorrelation("run-1", "task-1", agentId, "msg-1"),
                Map.of("k", "v")
        );
    }
}

