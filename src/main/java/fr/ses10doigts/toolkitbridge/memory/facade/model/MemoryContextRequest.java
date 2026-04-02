package fr.ses10doigts.toolkitbridge.memory.facade.model;

public record MemoryContextRequest(
        String agentId,
        String userId,
        String botId,
        String projectId,
        String currentUserMessage,
        String conversationId,
        Integer maxSemanticMemories,
        Integer maxEpisodes,
        Integer maxConversationMessages,
        Integer tokenBudgetHint
) {
    public MemoryContextRequest {
        if (agentId == null || agentId.isBlank()) {
            throw new IllegalArgumentException("agentId must not be blank");
        }
        if (conversationId == null || conversationId.isBlank()) {
            throw new IllegalArgumentException("conversationId must not be blank");
        }
        if (currentUserMessage != null && currentUserMessage.isBlank()) {
            currentUserMessage = null;
        }
        if (projectId != null && projectId.isBlank()) {
            projectId = null;
        }
    }
}
