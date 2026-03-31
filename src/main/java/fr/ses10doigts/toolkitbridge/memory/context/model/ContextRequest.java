package fr.ses10doigts.toolkitbridge.memory.context.model;

public record ContextRequest(
        String agentId,
        String conversationId,
        String projectId,
        String userMessage
) {
    public ContextRequest {
        if (agentId == null || agentId.isBlank()) {
            throw new IllegalArgumentException("agentId must not be blank");
        }
        if (conversationId == null || conversationId.isBlank()) {
            throw new IllegalArgumentException("conversationId must not be blank");
        }
        if (userMessage == null || userMessage.isBlank()) {
            throw new IllegalArgumentException("userMessage must not be blank");
        }
        if (projectId != null && projectId.isBlank()) {
            projectId = null;
        }
    }
}
