package fr.ses10doigts.toolkitbridge.service.agent.supervision.telegram;

import fr.ses10doigts.telegrambots.service.sender.TelegramSender;
import fr.ses10doigts.telegrambots.service.sender.TelegramSenderRegistry;
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentSensitiveAction;
import fr.ses10doigts.toolkitbridge.service.agent.supervision.human.HumanInterventionDecision;
import fr.ses10doigts.toolkitbridge.service.agent.supervision.human.HumanInterventionKind;
import fr.ses10doigts.toolkitbridge.service.agent.supervision.human.HumanInterventionRequest;
import fr.ses10doigts.toolkitbridge.service.agent.supervision.human.HumanInterventionStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TelegramHumanInterventionNotifierTest {

    @Test
    void publishesPreparedHumanReviewToTelegram() {
        TelegramSupervisionProperties properties = new TelegramSupervisionProperties();
        properties.setEnabled(true);
        properties.setBotId("supervision-bot");
        properties.setChatId(99L);

        TelegramSender sender = mock(TelegramSender.class);
        TelegramSenderRegistry senderRegistry = mock(TelegramSenderRegistry.class);
        when(senderRegistry.getRequiredSender("supervision-bot")).thenReturn(sender);

        TelegramHumanInterventionNotifier notifier = new TelegramHumanInterventionNotifier(
                properties,
                new TelegramSupervisionMessagePublisher(properties, java.util.Optional.of(senderRegistry), true)
        );
        HumanInterventionRequest request = new HumanInterventionRequest(
                "request-123456789",
                Instant.now(),
                "trace-123456789",
                "agent-7",
                AgentSensitiveAction.TOOL_EXECUTION,
                HumanInterventionKind.APPROVAL,
                HumanInterventionStatus.PENDING,
                "Review a deploy command",
                null,
                Map.of()
        );

        notifier.onRequestOpened(request);

        verify(sender).sendMessage(eq(99L), contains("Human review prepared"));
    }

    @Test
    void publishesRecordedDecisionToTelegram() {
        TelegramSupervisionProperties properties = new TelegramSupervisionProperties();
        properties.setEnabled(true);
        properties.setChatId(99L);

        TelegramSender sender = mock(TelegramSender.class);
        TelegramSenderRegistry senderRegistry = mock(TelegramSenderRegistry.class);
        when(senderRegistry.getDefaultBotSender()).thenReturn(sender);

        TelegramHumanInterventionNotifier notifier = new TelegramHumanInterventionNotifier(
                properties,
                new TelegramSupervisionMessagePublisher(properties, java.util.Optional.of(senderRegistry), true)
        );
        HumanInterventionRequest request = new HumanInterventionRequest(
                "request-1",
                Instant.now(),
                "trace-1",
                "agent-1",
                AgentSensitiveAction.DELEGATION,
                HumanInterventionKind.REVIEW,
                HumanInterventionStatus.APPROVED,
                "Review delegation",
                null,
                Map.of()
        );
        HumanInterventionDecision decision = new HumanInterventionDecision(
                "request-1",
                HumanInterventionStatus.APPROVED,
                Instant.now(),
                "operator-1",
                "telegram",
                "Approved",
                Map.of()
        );

        notifier.onDecisionRecorded(request, decision);

        verify(sender).sendMessage(eq(99L), contains("Human review decision recorded"));
    }
}
