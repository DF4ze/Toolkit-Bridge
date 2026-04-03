package fr.ses10doigts.toolkitbridge.service.agent.runtime.model;

import java.time.Instant;

public record AgentRuntimeState(
        String traceId,
        String channelType,
        String channelUserId,
        String channelConversationId,
        Instant startedAt
) {
}
