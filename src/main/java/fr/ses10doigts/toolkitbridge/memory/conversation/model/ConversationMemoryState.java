package fr.ses10doigts.toolkitbridge.memory.conversation.model;

import java.time.Instant;
import java.util.List;

public record ConversationMemoryState(
        String agentId,
        String conversationId,
        List<ConversationSummary> summaries,
        List<ConversationMessage> recentMessages,
        Instant updatedAt
) {
}

