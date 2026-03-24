package fr.ses10doigts.toolkitbridge.model.dto.agent.comm;

import java.util.Map;

public record AgentRequest(
        String agentId,
        String channelType,
        String channelUserId,
        String channelConversationId,
        String message,
        Map<String, Object> context
) {
}