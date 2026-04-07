package fr.ses10doigts.toolkitbridge.service.agent.supervision.telegram;

import fr.ses10doigts.toolkitbridge.service.agent.supervision.human.HumanInterventionDecision;
import fr.ses10doigts.toolkitbridge.service.agent.supervision.human.HumanInterventionNotifier;
import fr.ses10doigts.toolkitbridge.service.agent.supervision.human.HumanInterventionRequest;
import org.springframework.stereotype.Component;

@Component
public class TelegramHumanInterventionNotifier implements HumanInterventionNotifier {

    private final TelegramSupervisionProperties properties;
    private final TelegramSupervisionMessagePublisher publisher;

    public TelegramHumanInterventionNotifier(TelegramSupervisionProperties properties,
                                             TelegramSupervisionMessagePublisher publisher) {
        this.properties = properties;
        this.publisher = publisher;
    }

    @Override
    public void onRequestOpened(HumanInterventionRequest request) {
        if (!isEnabled()) {
            return;
        }
        publisher.publish(openedMessage(request));
    }

    @Override
    public void onDecisionRecorded(HumanInterventionRequest request, HumanInterventionDecision decision) {
        if (!isEnabled()) {
            return;
        }
        publisher.publish(decisionMessage(request, decision));
    }

    private boolean isEnabled() {
        return publisher.isConfigured() && properties.getHumanIntervention().isEnabled();
    }

    private String openedMessage(HumanInterventionRequest request) {
        StringBuilder builder = new StringBuilder("Human review prepared");
        appendLine(builder, "request", shorten(request.requestId()));
        appendLine(builder, "agent", request.agentId());
        appendLine(builder, "action", request.sensitiveAction().name());
        appendLine(builder, "kind", request.kind().name());
        appendLine(builder, "summary", request.summary());
        if (request.traceId() != null) {
            appendLine(builder, "trace", shorten(request.traceId()));
        }
        return builder.toString();
    }

    private String decisionMessage(HumanInterventionRequest request, HumanInterventionDecision decision) {
        StringBuilder builder = new StringBuilder("Human review decision recorded");
        appendLine(builder, "request", shorten(request.requestId()));
        appendLine(builder, "status", decision.status().name());
        if (decision.actorId() != null) {
            appendLine(builder, "actor", decision.actorId());
        }
        if (decision.channel() != null) {
            appendLine(builder, "channel", decision.channel());
        }
        if (decision.comment() != null) {
            appendLine(builder, "comment", decision.comment());
        }
        return builder.toString();
    }

    private void appendLine(StringBuilder builder, String label, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        builder.append(System.lineSeparator())
                .append(label)
                .append(": ")
                .append(value);
    }

    private String shorten(String value) {
        if (value == null || value.length() <= 12) {
            return value;
        }
        return value.substring(0, 12);
    }
}
