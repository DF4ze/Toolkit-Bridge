package fr.ses10doigts.toolkitbridge.service.agent.trace.persistence;

import fr.ses10doigts.toolkitbridge.ToolkitBridgeApplication;
import fr.ses10doigts.toolkitbridge.model.dto.admin.technical.TechnicalAdminView;
import fr.ses10doigts.toolkitbridge.service.admin.TraceQueryService;
import fr.ses10doigts.toolkitbridge.service.agent.trace.AgentTraceService;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceCorrelation;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = ToolkitBridgeApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.datasource.url=jdbc:sqlite:file:./target/critical-trace-it-${random.uuid}.db",
                "spring.datasource.driver-class-name=org.sqlite.JDBC",
                "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
                "spring.jpa.hibernate.ddl-auto=update",
                "spring.sql.init.mode=never",
                "telegram.enabled=false",
                "toolkit.observability.agent-tracing.memory.enabled=false",
                "toolkit.observability.agent-tracing.file.enabled=false",
                "toolkit.llm.openai-like.providers[0].name=seed",
                "toolkit.llm.openai-like.providers[0].base-url=http://localhost:11434/v1",
                "toolkit.llm.openai-like.providers[0].api-key=",
                "toolkit.llm.openai-like.providers[0].default-model=qwen3.5:9b"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CriticalTracePersistenceIT {

    @Autowired
    private AgentTraceService traceService;

    @Autowired
    private CriticalAgentTraceRepository repository;

    @Autowired
    private TraceQueryService traceQueryService;

    @BeforeEach
    void setUp() {
        repository.deleteAll();
    }

    @Test
    void pipelineShouldPersistOnlyRetainedTypesThroughDefaultTraceService() {
        AgentTraceCorrelation correlation = new AgentTraceCorrelation("run-1", "task-1", "agent-a", "msg-1");
        traceService.trace(AgentTraceEventType.TASK_STARTED, "task_orchestrator", correlation, Map.of("entryPoint", "TASK_ORCHESTRATOR"));
        traceService.trace(AgentTraceEventType.CONTEXT_ASSEMBLED, "task_orchestrator", correlation, Map.of("contextLength", 42));
        traceService.trace(AgentTraceEventType.ERROR, "task_orchestrator", correlation, Map.of("reason", "boom"));

        List<CriticalAgentTraceEntity> stored = repository.findByOrderByOccurredAtDescIdDesc(org.springframework.data.domain.PageRequest.of(0, 10));
        assertThat(stored).hasSize(2);
        assertThat(stored).extracting(CriticalAgentTraceEntity::getEventType)
                .containsExactly(AgentTraceEventType.ERROR, AgentTraceEventType.TASK_STARTED);
    }

    @Test
    void traceQueryServiceShouldReadDbWithSortFilterAndLimit() {
        repository.save(entity(Instant.parse("2026-01-01T10:00:00Z"), AgentTraceEventType.RESPONSE, "agent-z", "run-1"));
        repository.save(entity(Instant.parse("2026-01-01T10:00:00Z"), AgentTraceEventType.ERROR, "agent-z", "run-2"));
        repository.save(entity(Instant.parse("2026-01-01T09:00:00Z"), AgentTraceEventType.TOOL_CALL, "agent-y", "run-3"));

        List<TechnicalAdminView.TraceItem> recent = traceQueryService.listRecentTraces(2, null);
        List<TechnicalAdminView.TraceItem> byAgent = traceQueryService.listRecentTraces(10, "agent-z");

        assertThat(recent).hasSize(2);
        assertThat(recent.get(0).occurredAt()).isEqualTo(Instant.parse("2026-01-01T10:00:00Z"));
        assertThat(recent.get(1).occurredAt()).isEqualTo(Instant.parse("2026-01-01T10:00:00Z"));
        assertThat(recent.get(0).type()).isEqualTo(AgentTraceEventType.ERROR);
        assertThat(recent.get(1).type()).isEqualTo(AgentTraceEventType.RESPONSE);

        assertThat(byAgent).hasSize(2);
        assertThat(byAgent).extracting(TechnicalAdminView.TraceItem::agentId).containsOnly("agent-z");
    }

    @Test
    void repositoryShouldSortDeterministicallyByOccurredAtThenIdDesc() {
        CriticalAgentTraceEntity first = repository.save(entity(
                Instant.parse("2026-01-01T10:00:00Z"),
                AgentTraceEventType.RESPONSE,
                "agent-a",
                "run-a"
        ));
        CriticalAgentTraceEntity second = repository.save(entity(
                Instant.parse("2026-01-01T10:00:00Z"),
                AgentTraceEventType.ERROR,
                "agent-a",
                "run-b"
        ));

        List<CriticalAgentTraceEntity> ordered = repository.findByOrderByOccurredAtDescIdDesc(
                org.springframework.data.domain.PageRequest.of(0, 10)
        );

        assertThat(second.getId()).isGreaterThan(first.getId());
        assertThat(ordered.get(0).getId()).isEqualTo(second.getId());
        assertThat(ordered.get(1).getId()).isEqualTo(first.getId());
    }

    private CriticalAgentTraceEntity entity(Instant occurredAt,
                                            AgentTraceEventType type,
                                            String agentId,
                                            String runId) {
        CriticalAgentTraceEntity entity = new CriticalAgentTraceEntity();
        entity.setOccurredAt(occurredAt);
        entity.setEventType(type);
        entity.setSource("source");
        entity.setRunId(runId);
        entity.setAgentId(agentId);
        entity.setMessageId("msg-" + runId);
        entity.setTaskId("task-" + runId);
        entity.setAttributesJson("{\"k\":\"v\"}");
        entity.setIngestedAt(Instant.parse("2026-01-01T10:01:00Z"));
        return entity;
    }
}
