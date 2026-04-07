package fr.ses10doigts.toolkitbridge.service.agent.supervision.telegram;

import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEvent;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class TelegramTraceMessageFormatter {

    public Optional<String> format(AgentTraceEvent event) {
        if (event == null || event.type() == null) {
            return Optional.empty();
        }

        return switch (event.type()) {
            case DELEGATION -> delegation(event);
            case RESPONSE -> response(event);
            case ERROR -> error(event);
            default -> Optional.empty();
        };
    }

    private Optional<String> delegation(AgentTraceEvent event) {
        Map<String, Object> attributes = event.attributes();
        String status = stringValue(attributes.get("status"));
        String sender = stringValue(attributes.get("senderAgentId"));
        String target = firstNonBlank(
                stringValue(attributes.get("resolvedAgentId")),
                stringValue(attributes.get("recipientKind"))
        );
        String messageType = stringValue(attributes.get("messageType"));
        String responseFlag = booleanText(attributes.get("responseError"));

        StringBuilder builder = new StringBuilder("Inter-agent exchange");
        appendLine(builder, "status", defaultIfBlank(status, "observed"));
        appendLine(builder, "from", defaultIfBlank(sender, "unknown"));
        appendLine(builder, "to", defaultIfBlank(target, "unknown"));
        if (messageType != null) {
            appendLine(builder, "message", messageType);
        }
        if (responseFlag != null) {
            appendLine(builder, "response", responseFlag);
        }
        appendTrace(builder, event);
        return Optional.of(builder.toString());
    }

    private Optional<String> response(AgentTraceEvent event) {
        String agentId = event.correlation() == null ? null : event.correlation().agentId();
        Integer responseLength = intValue(event.attributes().get("responseLength"));

        StringBuilder builder = new StringBuilder("Agent response");
        appendLine(builder, "agent", defaultIfBlank(agentId, "unknown"));
        appendLine(builder, "source", event.source());
        if (responseLength != null) {
            appendLine(builder, "length", String.valueOf(responseLength));
        }
        appendTrace(builder, event);
        return Optional.of(builder.toString());
    }

    private Optional<String> error(AgentTraceEvent event) {
        Map<String, Object> attributes = event.attributes();
        String reason = firstNonBlank(
                stringValue(attributes.get("reason")),
                stringValue(attributes.get("details"))
        );
        String category = stringValue(attributes.get("category"));
        String status = stringValue(attributes.get("status"));
        String agentId = event.correlation() == null ? null : event.correlation().agentId();

        StringBuilder builder = new StringBuilder("Agent error");
        appendLine(builder, "source", defaultIfBlank(event.source(), "unknown"));
        if (category != null) {
            appendLine(builder, "category", category);
        }
        if (status != null) {
            appendLine(builder, "status", status);
        }
        if (agentId != null) {
            appendLine(builder, "agent", agentId);
        }
        appendLine(builder, "reason", defaultIfBlank(reason, "unexpected failure"));
        appendTrace(builder, event);
        return Optional.of(builder.toString());
    }

    private void appendTrace(StringBuilder builder, AgentTraceEvent event) {
        String traceId = event.correlation() == null ? null : event.correlation().runId();
        if (traceId != null) {
            appendLine(builder, "trace", shorten(traceId));
        }
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

    private String booleanText(Object raw) {
        if (!(raw instanceof Boolean value)) {
            return null;
        }
        return value ? "error" : "ok";
    }

    private Integer intValue(Object raw) {
        if (raw instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private String shorten(String value) {
        if (value == null || value.length() <= 12) {
            return value;
        }
        return value.substring(0, 12);
    }

    private String stringValue(Object raw) {
        if (raw == null) {
            return null;
        }
        String value = String.valueOf(raw).trim();
        return value.isEmpty() ? null : value;
    }

    private String firstNonBlank(String first, String second) {
        return first != null ? first : second;
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
