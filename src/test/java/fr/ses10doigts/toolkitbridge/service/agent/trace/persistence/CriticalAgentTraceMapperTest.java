package fr.ses10doigts.toolkitbridge.service.agent.trace.persistence;

import fr.ses10doigts.toolkitbridge.service.agent.trace.config.CriticalTraceSanitizationProperties;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceCorrelation;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEvent;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEventType;
import org.junit.jupiter.api.Test;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CriticalAgentTraceMapperTest {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CriticalAgentTraceMapper mapper = new CriticalAgentTraceMapper(
            objectMapper,
            new CriticalTraceSanitizationProperties()
    );

    @Test
    void shouldSanitizeToolCallMessage() throws Exception {
        String longMessage = "x".repeat(400);
        AgentTraceEvent event = new AgentTraceEvent(
                Instant.parse("2026-04-10T10:00:00Z"),
                AgentTraceEventType.TOOL_CALL,
                "llm_service",
                new AgentTraceCorrelation("run-1", "task-1", "agent-1", "msg-1"),
                Map.of(
                        "toolName", "read_file",
                        "message", longMessage,
                        "argumentKeys", java.util.List.of("path", "mode")
                )
        );

        CriticalAgentTraceEntity entity = mapper.toEntity(event);
        Map<String, Object> stored = objectMapper.readValue(entity.getAttributesJson(), MAP_TYPE);

        assertThat(stored.get("toolName")).isEqualTo("read_file");
        assertThat(stored.get("argumentKeys")).isEqualTo(java.util.List.of("path", "mode"));
        assertThat(((String) stored.get("message")).length()).isLessThan(longMessage.length());
    }

    @Test
    void shouldSanitizeErrorReason() throws Exception {
        String longReason = "r".repeat(900);
        AgentTraceEvent event = new AgentTraceEvent(
                Instant.parse("2026-04-10T10:00:00Z"),
                AgentTraceEventType.ERROR,
                "task_orchestrator",
                new AgentTraceCorrelation("run-1", null, "agent-2", null),
                Map.of("category", "provider", "reason", longReason)
        );

        CriticalAgentTraceEntity entity = mapper.toEntity(event);
        Map<String, Object> stored = objectMapper.readValue(entity.getAttributesJson(), MAP_TYPE);

        assertThat(stored.get("category")).isEqualTo("provider");
        assertThat(((String) stored.get("reason")).length()).isLessThan(longReason.length());
    }

    @Test
    void shouldHandlePartialCorrelationWithoutRigidConstraints() {
        AgentTraceEvent event = new AgentTraceEvent(
                Instant.parse("2026-04-10T10:00:00Z"),
                AgentTraceEventType.RESPONSE,
                "chat_orchestrator",
                new AgentTraceCorrelation(null, null, " Agent-3 ", null),
                Map.of("success", true)
        );

        CriticalAgentTraceEntity entity = mapper.toEntity(event);
        assertThat(entity.getRunId()).isNull();
        assertThat(entity.getTaskId()).isNull();
        assertThat(entity.getMessageId()).isNull();
        assertThat(entity.getAgentId()).isEqualTo("agent-3");
    }

    @Test
    void shouldExposeCriticalTypePredicate() {
        assertThat(mapper.isCriticalType(AgentTraceEventType.ERROR)).isTrue();
        assertThat(mapper.isCriticalType(AgentTraceEventType.CONTEXT_ASSEMBLED)).isFalse();
    }

    @Test
    void parseAttributesJsonShouldFallbackSafelyOnInvalidJson() {
        assertThat(mapper.parseAttributesJson("{invalid json")).isEqualTo(Map.of());
    }
}
