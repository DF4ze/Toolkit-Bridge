package fr.ses10doigts.toolkitbridge.memory.context.model;

public record ContextRequest(
        String agentId,
        String userId,
        String conversationId,
        String projectId,
        String currentUserMessage,
        Integer maxSemanticMemories,
        Integer maxEpisodes
) {
    public ContextRequest {
        agentId = normalize(agentId);
        userId = normalize(userId);
        conversationId = normalize(conversationId);
        projectId = normalize(projectId);
        currentUserMessage = normalize(currentUserMessage);

        if (agentId == null || agentId.isBlank()) {
            throw new IllegalArgumentException("agentId must not be blank");
        }
        if (conversationId == null || conversationId.isBlank()) {
            throw new IllegalArgumentException("conversationId must not be blank");
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
    }

    public ContextRequest(
            String agentId,
            String conversationId,
            String projectId,
            String currentUserMessage
    ) {
        this(agentId, null, conversationId, projectId, currentUserMessage, null, null);
    }

    public ContextRequest(
            String agentId,
            String conversationId,
            String projectId,
            String currentUserMessage,
            Integer maxSemanticMemories,
            Integer maxEpisodes
    ) {
        this(agentId, null, conversationId, projectId, currentUserMessage, maxSemanticMemories, maxEpisodes);
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
