package fr.ses10doigts.toolkitbridge.service.agent.trace.sink;

import fr.ses10doigts.toolkitbridge.service.agent.trace.config.CriticalTraceSanitizationProperties;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceCorrelation;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEvent;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEventType;
import fr.ses10doigts.toolkitbridge.service.agent.trace.persistence.CriticalAgentTraceEntity;
import fr.ses10doigts.toolkitbridge.service.agent.trace.persistence.CriticalAgentTraceMapper;
import fr.ses10doigts.toolkitbridge.service.agent.trace.persistence.CriticalAgentTraceRepository;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CriticalTraceJpaSinkTest {

    @Test
    void shouldPersistRetainedType() {
        CriticalAgentTraceRepository repository = mock(CriticalAgentTraceRepository.class);
        CriticalAgentTraceMapper mapper = spy(new CriticalAgentTraceMapper(new ObjectMapper(), new CriticalTraceSanitizationProperties()));
        when(repository.save(any(CriticalAgentTraceEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CriticalTraceJpaSink sink = new CriticalTraceJpaSink(repository, mapper);
        sink.publish(event(AgentTraceEventType.ERROR));

        verify(repository).save(any(CriticalAgentTraceEntity.class));
        verify(mapper).toEntity(any(AgentTraceEvent.class));
    }

    @Test
    void shouldIgnoreNonRetainedType() {
        CriticalAgentTraceRepository repository = mock(CriticalAgentTraceRepository.class);
        CriticalAgentTraceMapper mapper = spy(new CriticalAgentTraceMapper(new ObjectMapper(), new CriticalTraceSanitizationProperties()));
        CriticalTraceJpaSink sink = new CriticalTraceJpaSink(repository, mapper);

        sink.publish(event(AgentTraceEventType.CONTEXT_ASSEMBLED));

        verify(repository, never()).save(any(CriticalAgentTraceEntity.class));
        verify(mapper, never()).toEntity(any(AgentTraceEvent.class));
    }

    private AgentTraceEvent event(AgentTraceEventType type) {
        return new AgentTraceEvent(
                Instant.parse("2026-04-10T10:00:00Z"),
                type,
                "source",
                new AgentTraceCorrelation("run-1", "task-1", "agent-1", "msg-1"),
                Map.of("k", "v")
        );
    }
}
