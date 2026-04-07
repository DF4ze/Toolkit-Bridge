package fr.ses10doigts.toolkitbridge.service.agent.communication.model;

import fr.ses10doigts.toolkitbridge.persistence.model.DurableObject;
import fr.ses10doigts.toolkitbridge.persistence.model.PersistableObjectFamily;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AgentMessage(
        String messageId,
        String correlationId,
        String senderAgentId,
        AgentRecipient recipient,
        Instant timestamp,
        AgentMessageType type,
        AgentMessagePayload payload
) implements DurableObject {

    public AgentMessage {
        if (messageId == null || messageId.isBlank()) {
            throw new IllegalArgumentException("messageId must not be blank");
        }
        if (correlationId == null || correlationId.isBlank()) {
            throw new IllegalArgumentException("correlationId must not be blank");
        }
        if (senderAgentId == null || senderAgentId.isBlank()) {
            throw new IllegalArgumentException("senderAgentId must not be blank");
        }
        Objects.requireNonNull(recipient, "recipient must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(payload, "payload must not be null");
    }

    public static AgentMessage create(String senderAgentId,
                                      AgentRecipient recipient,
                                      AgentMessageType type,
                                      AgentMessagePayload payload) {
        return new AgentMessage(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                senderAgentId,
                recipient,
                Instant.now(),
                type,
                payload
        );
    }

    public static AgentMessage create(String correlationId,
                                      String senderAgentId,
                                      AgentRecipient recipient,
                                      AgentMessageType type,
                                      AgentMessagePayload payload) {
        String safeCorrelationId = (correlationId == null || correlationId.isBlank())
                ? UUID.randomUUID().toString()
                : correlationId;
        return new AgentMessage(
                UUID.randomUUID().toString(),
                safeCorrelationId,
                senderAgentId,
                recipient,
                Instant.now(),
                type,
                payload
        );
    }

    @Override
    public PersistableObjectFamily persistableFamily() {
        return PersistableObjectFamily.MESSAGE;
    }
}
