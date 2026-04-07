package fr.ses10doigts.toolkitbridge.service.agent.supervision.telegram;

import fr.ses10doigts.telegrambots.service.sender.TelegramSender;
import fr.ses10doigts.telegrambots.service.sender.TelegramSenderRegistry;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceCorrelation;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEvent;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEventType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TelegramAgentTraceSinkTest {

    @Test
    void publishesFormattedTraceToConfiguredTelegramChat() {
        TelegramSupervisionProperties properties = new TelegramSupervisionProperties();
        properties.setEnabled(true);
        properties.setBotId("supervision-bot");
        properties.setChatId(42L);

        TelegramSender sender = mock(TelegramSender.class);
        TelegramSenderRegistry senderRegistry = mock(TelegramSenderRegistry.class);
        when(senderRegistry.getRequiredSender("supervision-bot")).thenReturn(sender);

        TelegramAgentTraceSink sink = new TelegramAgentTraceSink(
                properties,
                new TelegramTraceMessageFormatter(),
                new TelegramSupervisionMessagePublisher(properties, java.util.Optional.of(senderRegistry), true)
        );

        sink.publish(new AgentTraceEvent(
                Instant.now(),
                AgentTraceEventType.DELEGATION,
                "message_bus",
                new AgentTraceCorrelation("trace-1", null, "critic", "message-1"),
                Map.of(
                        "status", "resolved",
                        "senderAgentId", "planner",
                        "resolvedAgentId", "critic",
                        "messageType", "QUESTION"
                )
        ));

        verify(sender).sendMessage(eq(42L), contains("Inter-agent exchange"));
    }

    @Test
    void ignoresEventsNotSelectedForTelegramPublishing() {
        TelegramSupervisionProperties properties = new TelegramSupervisionProperties();
        properties.setEnabled(true);
        properties.setChatId(42L);

        TelegramSender sender = mock(TelegramSender.class);
        TelegramSenderRegistry senderRegistry = mock(TelegramSenderRegistry.class);
        when(senderRegistry.getDefaultBotSender()).thenReturn(sender);

        TelegramAgentTraceSink sink = new TelegramAgentTraceSink(
                properties,
                new TelegramTraceMessageFormatter(),
                new TelegramSupervisionMessagePublisher(properties, java.util.Optional.of(senderRegistry), true)
        );

        sink.publish(new AgentTraceEvent(
                Instant.now(),
                AgentTraceEventType.TOOL_CALL,
                "tool_runner",
                null,
                Map.of()
        ));

        verify(sender, never()).sendMessage(anyLong(), anyString());
    }
}
