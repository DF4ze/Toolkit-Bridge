package fr.ses10doigts.toolkitbridge.service.agent.supervision.telegram;

import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceCorrelation;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEvent;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEventType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class TelegramTraceMessageFormatterTest {

    private final TelegramTraceMessageFormatter formatter = new TelegramTraceMessageFormatter();

    @Test
    void formatsDelegationEventForHumanMonitoring() {
        AgentTraceEvent event = new AgentTraceEvent(
                Instant.parse("2026-04-07T10:15:30Z"),
                AgentTraceEventType.DELEGATION,
                "message_bus",
                new AgentTraceCorrelation("trace-123456789", null, "agent-target", "message-1"),
                Map.of(
                        "status", "delivered",
                        "senderAgentId", "planner",
                        "resolvedAgentId", "critic",
                        "messageType", "QUESTION",
                        "responseError", false
                )
        );

        String message = formatter.format(event).orElseThrow();

        assertThat(message).contains("Inter-agent exchange");
        assertThat(message).contains("status: delivered");
        assertThat(message).contains("from: planner");
        assertThat(message).contains("to: critic");
        assertThat(message).contains("message: QUESTION");
        assertThat(message).contains("response: ok");
        assertThat(message).contains("trace: trace-123456");
    }

    @Test
    void formatsErrorEventWithReason() {
        AgentTraceEvent event = new AgentTraceEvent(
                Instant.now(),
                AgentTraceEventType.ERROR,
                "message_bus",
                new AgentTraceCorrelation("trace-1", null, "agent-2", "message-9"),
                Map.of(
                        "category", "delegation",
                        "status", "dispatch_failed",
                        "reason", "Dispatch execution failed"
                )
        );

        String message = formatter.format(event).orElseThrow();

        assertThat(message).contains("Agent error");
        assertThat(message).contains("source: message_bus");
        assertThat(message).contains("category: delegation");
        assertThat(message).contains("status: dispatch_failed");
        assertThat(message).contains("agent: agent-2");
        assertThat(message).contains("reason: Dispatch execution failed");
    }

    @Test
    void ignoresUnsupportedEventTypes() {
        AgentTraceEvent event = new AgentTraceEvent(
                Instant.now(),
                AgentTraceEventType.TOOL_CALL,
                "tool_runner",
                null,
                Map.of()
        );

        assertThat(formatter.format(event)).isEmpty();
    }
}
