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
        agentId = normalize(agentId);
        userId = normalize(userId);
        botId = normalize(botId);
        projectId = normalize(projectId);
        currentUserMessage = normalize(currentUserMessage);
        conversationId = normalize(conversationId);

        if (agentId == null || agentId.isBlank()) {
            throw new IllegalArgumentException("agentId must not be blank");
        }
        if (conversationId == null || conversationId.isBlank()) {
            throw new IllegalArgumentException("conversationId must not be blank");
        }
        if (maxSemanticMemories != null && maxSemanticMemories <= 0) {
            throw new IllegalArgumentException("maxSemanticMemories must be > 0");
        }
        if (maxEpisodes != null && maxEpisodes <= 0) {
            throw new IllegalArgumentException("maxEpisodes must be > 0");
        }
        if (maxConversationMessages != null && maxConversationMessages <= 0) {
            throw new IllegalArgumentException("maxConversationMessages must be > 0");
        }
        if (tokenBudgetHint != null && tokenBudgetHint <= 0) {
            throw new IllegalArgumentException("tokenBudgetHint must be > 0");
        }
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
