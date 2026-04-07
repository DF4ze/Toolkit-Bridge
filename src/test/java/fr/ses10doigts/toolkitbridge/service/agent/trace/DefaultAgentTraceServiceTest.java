package fr.ses10doigts.toolkitbridge.service.agent.trace;

import fr.ses10doigts.toolkitbridge.service.agent.trace.config.AgentTraceProperties;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceCorrelation;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEvent;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEventType;
import fr.ses10doigts.toolkitbridge.service.agent.trace.sink.AgentTraceSink;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultAgentTraceServiceTest {

    @Test
    void publishesStructuredEventsToAllConfiguredSinks() {
        AgentTraceProperties properties = new AgentTraceProperties();
        RecordingSink first = new RecordingSink();
        RecordingSink second = new RecordingSink();
        DefaultAgentTraceService service = new DefaultAgentTraceService(List.of(first, second), properties);

        service.trace(
                AgentTraceEventType.RESPONSE,
                "chat_orchestrator",
                new AgentTraceCorrelation("run-1", "task-1", "agent-1", "message-1"),
                Map.of("responseLength", 42, "success", true)
        );

        assertThat(first.events).hasSize(1);
        assertThat(second.events).hasSize(1);
        AgentTraceEvent event = first.events.get(0);
        assertThat(event.type()).isEqualTo(AgentTraceEventType.RESPONSE);
        assertThat(event.source()).isEqualTo("chat_orchestrator");
        assertThat(event.correlation().runId()).isEqualTo("run-1");
        assertThat(event.attributes()).containsEntry("responseLength", 42);
        assertThat(event.attributes()).containsEntry("success", true);
    }

    @Test
    void ignoresTracePublishingWhenObservabilityIsDisabled() {
        AgentTraceProperties properties = new AgentTraceProperties();
        properties.setEnabled(false);
        RecordingSink sink = new RecordingSink();
        DefaultAgentTraceService service = new DefaultAgentTraceService(List.of(sink), properties);

        service.trace(AgentTraceEventType.ERROR, "message_bus", null, Map.of("reason", "boom"));

        assertThat(sink.events).isEmpty();
    }

    private static class RecordingSink implements AgentTraceSink {
        private final List<AgentTraceEvent> events = new ArrayList<>();

        @Override
        public void publish(AgentTraceEvent event) {
            events.add(event);
        }
    }
}
