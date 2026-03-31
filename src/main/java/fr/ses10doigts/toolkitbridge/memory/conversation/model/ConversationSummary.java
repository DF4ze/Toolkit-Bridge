package fr.ses10doigts.toolkitbridge.memory.conversation.model;

import java.time.Instant;
import java.util.UUID;

public record ConversationSummary(
        UUID id,
        String agentId,
        String conversationId,
        String content,
        int summarizedMessageCount,
        Instant fromTimestamp,
        Instant toTimestamp,
        Instant createdAt,
        ConversationCompressionReason reason
) {
}

