package fr.ses10doigts.toolkitbridge.service.agent.communication.model;

import java.util.Map;

public record AgentMessagePayload(
        String text,
        String channelType,
        String channelUserId,
        String channelConversationId,
        String projectId,
        Map<String, Object> context
) {

    public AgentMessagePayload {
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        context = context == null ? Map.of() : Map.copyOf(context);
    }
}

