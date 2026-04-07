package fr.ses10doigts.toolkitbridge.memory.conversation.model;

import fr.ses10doigts.toolkitbridge.persistence.model.EphemeralObject;

import java.time.Instant;
import java.util.List;

public record ConversationMemoryState(
        String agentId,
        String conversationId,
        List<ConversationSummary> summaries,
        List<ConversationMessage> recentMessages,
        Instant updatedAt
) implements EphemeralObject {
}
