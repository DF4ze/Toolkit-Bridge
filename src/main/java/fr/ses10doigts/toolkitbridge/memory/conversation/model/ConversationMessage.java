package fr.ses10doigts.toolkitbridge.memory.conversation.model;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record ConversationMessage(
        UUID id,
        String agentId,
        String conversationId,
        ConversationRole role,
        String content,
        Instant createdAt,
        Map<String, Object> metadata
) {
}

