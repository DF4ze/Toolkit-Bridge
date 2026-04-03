package fr.ses10doigts.toolkitbridge.service.agent.communication.bus;

import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentResponse;

import java.util.Objects;

public record AgentMessageDispatchResult(
        String messageId,
        String correlationId,
        AgentMessageDispatchStatus status,
        String resolvedAgentId,
        AgentResponse response,
        String details
) {

    public AgentMessageDispatchResult {
        if (messageId == null || messageId.isBlank()) {
            throw new IllegalArgumentException("messageId must not be blank");
        }
        if (correlationId == null || correlationId.isBlank()) {
            throw new IllegalArgumentException("correlationId must not be blank");
        }
        Objects.requireNonNull(status, "status must not be null");
    }

    public static AgentMessageDispatchResult delivered(String messageId,
                                                       String correlationId,
                                                       String resolvedAgentId,
                                                       AgentResponse response) {
        return new AgentMessageDispatchResult(
                messageId,
                correlationId,
                AgentMessageDispatchStatus.DELIVERED,
                resolvedAgentId,
                response,
                null
        );
    }

    public static AgentMessageDispatchResult unroutable(String messageId, String correlationId, String details) {
        return new AgentMessageDispatchResult(
                messageId,
                correlationId,
                AgentMessageDispatchStatus.UNROUTABLE,
                null,
                null,
                details
        );
    }

    public static AgentMessageDispatchResult failed(String messageId,
                                                    String correlationId,
                                                    String resolvedAgentId,
                                                    String details) {
        return new AgentMessageDispatchResult(
                messageId,
                correlationId,
                AgentMessageDispatchStatus.FAILED,
                resolvedAgentId,
                null,
                details
        );
    }
}

