package fr.ses10doigts.toolkitbridge.memory.facade;

import fr.ses10doigts.toolkitbridge.memory.context.model.ContextRequest;
import fr.ses10doigts.toolkitbridge.memory.context.port.ContextAssembler;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMemoryKey;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMessage;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationRole;
import fr.ses10doigts.toolkitbridge.memory.conversation.port.ConversationMemoryService;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeEvent;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeEventType;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeScope;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeStatus;
import fr.ses10doigts.toolkitbridge.memory.episodic.service.EpisodicMemoryService;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultMemoryFacade implements MemoryFacade {

    private final ContextAssembler contextAssembler;
    private final ConversationMemoryService conversationMemoryService;
    private final EpisodicMemoryService episodicMemoryService;

    @Override
    public String buildContext(ContextRequest request) {
        return contextAssembler.buildContext(request);
    }

    @Override
    public void onUserMessage(ConversationMemoryKey key, String content, Map<String, Object> metadata) {
        conversationMemoryService.appendMessage(
                key,
                new ConversationMessage(
                        UUID.randomUUID(),
                        key.agentId(),
                        key.conversationId(),
                        ConversationRole.USER,
                        content,
                        Instant.now(),
                        safeMetadata(metadata)
                )
        );
    }

    @Override
    public void onAssistantMessage(ConversationMemoryKey key, String content, Map<String, Object> metadata) {
        conversationMemoryService.appendMessage(
                key,
                new ConversationMessage(
                        UUID.randomUUID(),
                        key.agentId(),
                        key.conversationId(),
                        ConversationRole.ASSISTANT,
                        content,
                        Instant.now(),
                        safeMetadata(metadata)
                )
        );

        EpisodeEvent event = new EpisodeEvent();
        event.setAgentId(key.agentId());
        event.setScope(EpisodeScope.AGENT);
        event.setScopeId(key.conversationId());
        event.setType(EpisodeEventType.RESULT);
        event.setAction("agent_exchange");
        event.setDetails("response_length=" + (content == null ? 0 : content.length()));
        event.setStatus(EpisodeStatus.SUCCESS);
        recordEpisodeSafely(event);
    }

    @Override
    public void onToolExecution(String agentId, String conversationId, String action, String details, EpisodeStatus status) {
        EpisodeEvent event = new EpisodeEvent();
        event.setAgentId(agentId);
        event.setScope(EpisodeScope.AGENT);
        event.setScopeId(conversationId);
        event.setType(status == EpisodeStatus.FAILURE ? EpisodeEventType.ERROR : EpisodeEventType.ACTION);
        event.setAction(action == null || action.isBlank() ? "tool_execution" : action);
        event.setDetails(details);
        event.setStatus(status == null ? EpisodeStatus.UNKNOWN : status);
        recordEpisodeSafely(event);
    }

    @Override
    public void markContextMemoriesUsed(String agentId, String conversationId, List<MemoryEntry> memories) {
        // Intentionally no-op for now. This hook is ready when context injection surfaces memory IDs.
    }

    private Map<String, Object> safeMetadata(Map<String, Object> metadata) {
        if (metadata == null || metadata.isEmpty()) {
            return Map.of();
        }
        return Map.copyOf(metadata);
    }

    private void recordEpisodeSafely(EpisodeEvent event) {
        try {
            episodicMemoryService.record(event);
        } catch (Exception e) {
            log.warn("Failed to record episodic event for agent={}", event.getAgentId(), e);
        }
    }
}
