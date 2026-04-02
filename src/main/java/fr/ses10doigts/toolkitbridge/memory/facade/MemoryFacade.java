package fr.ses10doigts.toolkitbridge.memory.facade;

import fr.ses10doigts.toolkitbridge.memory.context.model.ContextRequest;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMemoryKey;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeStatus;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;

import java.util.List;
import java.util.Map;

public interface MemoryFacade {

    String buildContext(ContextRequest request);

    void onUserMessage(ConversationMemoryKey key, String content, Map<String, Object> metadata);

    void onAssistantMessage(ConversationMemoryKey key, String content, Map<String, Object> metadata);

    void onToolExecution(String agentId, String conversationId, String action, String details, EpisodeStatus status);

    void markContextMemoriesUsed(String agentId, String conversationId, List<MemoryEntry> memories);
}
