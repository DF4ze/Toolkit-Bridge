package fr.ses10doigts.toolkitbridge.memory.tool.model;

public record MemoryContextRecallRequest(
        String agentId,
        String userId,
        String projectId,
        String conversationId,
        String focus,
        Integer maxSemanticMemories,
        Integer maxEpisodes
) {
}
