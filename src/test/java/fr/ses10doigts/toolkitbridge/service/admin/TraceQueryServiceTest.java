package fr.ses10doigts.toolkitbridge.service.admin;

import fr.ses10doigts.toolkitbridge.model.dto.admin.technical.TechnicalAdminView;
import fr.ses10doigts.toolkitbridge.service.admin.config.AdminTechnicalProperties;
import fr.ses10doigts.toolkitbridge.service.agent.trace.config.CriticalTraceSanitizationProperties;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEventType;
import fr.ses10doigts.toolkitbridge.service.agent.trace.persistence.CriticalAgentTraceEntity;
import fr.ses10doigts.toolkitbridge.service.agent.trace.persistence.CriticalAgentTraceMapper;
import fr.ses10doigts.toolkitbridge.service.agent.trace.persistence.CriticalAgentTraceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TraceQueryServiceTest {

    private final CriticalAgentTraceMapper mapper = new CriticalAgentTraceMapper(
            new ObjectMapper(),
            new CriticalTraceSanitizationProperties()
    );

    @Test
    void listRecentTracesUsesGlobalDbSourceWhenAgentIdIsBlank() {
        CriticalAgentTraceRepository repository = mock(CriticalAgentTraceRepository.class);
        AdminTechnicalProperties properties = new AdminTechnicalProperties();
        properties.setMaxListLimit(2);

        when(repository.findByOrderByOccurredAtDescIdDesc(any(Pageable.class))).thenReturn(List.of(
                trace(1L, Instant.parse("2026-01-01T10:00:00Z"), AgentTraceEventType.ERROR, "agent-1"),
                trace(2L, Instant.parse("2026-01-01T09:00:00Z"), AgentTraceEventType.RESPONSE, "agent-2")
        ));

        TraceQueryService service = new TraceQueryService(repository, mapper, properties);

        List<TechnicalAdminView.TraceItem> items = service.listRecentTraces(999, "   ");

        assertThat(items).hasSize(2);
        assertThat(items.get(0).type()).isEqualTo(AgentTraceEventType.ERROR);
        assertThat(items.get(0).agentId()).isEqualTo("agent-1");
        verify(repository).findByOrderByOccurredAtDescIdDesc(any(Pageable.class));
        verify(repository, never()).findByAgentIdOrderByOccurredAtDescIdDesc(any(), any(Pageable.class));
    }

    @Test
    void listRecentTracesUsesAgentSpecificDbSourceWhenAgentIdIsProvided() {
        CriticalAgentTraceRepository repository = mock(CriticalAgentTraceRepository.class);
        AdminTechnicalProperties properties = new AdminTechnicalProperties();

        when(repository.findByAgentIdOrderByOccurredAtDescIdDesc(eq("agent-9"), any(Pageable.class))).thenReturn(List.of(
                trace(3L, Instant.parse("2026-01-01T10:00:00Z"), AgentTraceEventType.RESPONSE, "agent-9")
        ));

        TraceQueryService service = new TraceQueryService(repository, mapper, properties);

        List<TechnicalAdminView.TraceItem> items = service.listRecentTraces(10, " Agent-9 ");

        assertThat(items).hasSize(1);
        assertThat(items.get(0).agentId()).isEqualTo("agent-9");
        assertThat(items.get(0).runId()).isEqualTo("run-1");
        verify(repository).findByAgentIdOrderByOccurredAtDescIdDesc(eq("agent-9"), any(Pageable.class));
        verify(repository, never()).findByOrderByOccurredAtDescIdDesc(any(Pageable.class));
    }

    private CriticalAgentTraceEntity trace(Long id, Instant occurredAt, AgentTraceEventType type, String agentId) {
        CriticalAgentTraceEntity entity = new CriticalAgentTraceEntity();
        entity.setId(id);
        entity.setOccurredAt(occurredAt);
        entity.setEventType(type);
        entity.setSource("source");
        entity.setRunId("run-1");
        entity.setAgentId(agentId);
        entity.setMessageId("msg-1");
        entity.setTaskId("task-1");
        entity.setAttributesJson("{\"k\":\"v\"}");
        entity.setIngestedAt(Instant.parse("2026-01-01T10:01:00Z"));
        return entity;
    }
}
