package fr.ses10doigts.toolkitbridge.memory.context.model;

public record ContextRequest(
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
    public ContextRequest {
        agentId = normalize(agentId);
        currentUserMessage = normalize(currentUserMessage);
        userId = normalize(userId);
        botId = normalize(botId);
        conversationId = normalize(conversationId);
        projectId = normalize(projectId);

        if (agentId == null || agentId.isBlank()) {
            throw new IllegalArgumentException("agentId must not be blank");
        }
        if (currentUserMessage == null || currentUserMessage.isBlank()) {
            throw new IllegalArgumentException("currentUserMessage must not be blank");
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

    public ContextRequest(
            String agentId,
            String conversationId,
            String projectId,
            String currentUserMessage
    ) {
        this(agentId,
                null,
                null,
                projectId,
                currentUserMessage,
                conversationId,
                null,
                null,
                null,
                null);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
