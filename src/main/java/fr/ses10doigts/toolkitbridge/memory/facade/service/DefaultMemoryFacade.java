package fr.ses10doigts.toolkitbridge.memory.facade.service;

import fr.ses10doigts.toolkitbridge.memory.context.model.AssembledContext;
import fr.ses10doigts.toolkitbridge.memory.context.model.ContextRequest;
import fr.ses10doigts.toolkitbridge.memory.context.port.ContextAssembler;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMemoryKey;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMessage;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationRole;
import fr.ses10doigts.toolkitbridge.memory.conversation.port.ConversationMemoryService;
import fr.ses10doigts.toolkitbridge.memory.episodic.factory.EpisodicEventFactory;
import fr.ses10doigts.toolkitbridge.memory.episodic.service.EpisodicMemoryService;
import fr.ses10doigts.toolkitbridge.memory.facade.MemoryFacade;
import fr.ses10doigts.toolkitbridge.memory.facade.model.MemoryContext;
import fr.ses10doigts.toolkitbridge.memory.facade.model.MemoryContextRequest;
import fr.ses10doigts.toolkitbridge.memory.facade.model.ToolExecutionRecord;
import fr.ses10doigts.toolkitbridge.memory.integration.config.MemoryIntegrationProperties;
import fr.ses10doigts.toolkitbridge.memory.integration.service.ImplicitMemoryWritePipeline;
import fr.ses10doigts.toolkitbridge.memory.retrieval.model.RetrievedMemories;
import fr.ses10doigts.toolkitbridge.memory.retrieval.facade.MemoryRetrievalFacade;
import fr.ses10doigts.toolkitbridge.memory.semantic.service.SemanticMemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultMemoryFacade implements MemoryFacade {

    private final MemoryRetrievalFacade memoryRetrievalFacade;
    private final ContextAssembler contextAssembler;
    private final ConversationMemoryService conversationMemoryService;
    private final EpisodicMemoryService episodicMemoryService;
    private final SemanticMemoryService semanticMemoryService;
    private final ImplicitMemoryWritePipeline implicitMemoryWritePipeline;
    private final EpisodicEventFactory episodicEventFactory;
    private final MemoryIntegrationProperties properties;

    @Override
    public MemoryContext buildContext(MemoryContextRequest request) {
        validateRequest(request);

        ContextRequest contextRequest = toContextRequest(request);
        RetrievedMemories retrieved = memoryRetrievalFacade.retrieve(contextRequest);
        AssembledContext assembled = contextAssembler.buildContext(contextRequest, retrieved);
        return new MemoryContext(assembled.text(), assembled.injectedSemanticMemoryIds());
    }

    @Override
    public void onUserMessage(MemoryContextRequest request) {
        validateRequest(request);
        if (request.currentUserMessage() == null || request.currentUserMessage().isBlank()) {
            return;
        }

        appendConversationMessage(request, ConversationRole.USER, request.currentUserMessage());
        recordEpisode(request, episodicEventFactory.userMessageReceived(request));
        writeImplicitMemories(request, request.currentUserMessage(), "user");
    }

    @Override
    public void onAssistantMessage(MemoryContextRequest request, String assistantMessage) {
        validateRequest(request);
        if (assistantMessage == null || assistantMessage.isBlank()) {
            return;
        }

        appendConversationMessage(request, ConversationRole.ASSISTANT, assistantMessage);
        recordEpisode(request, episodicEventFactory.assistantResponseGenerated(request, assistantMessage));
        writeImplicitMemories(request, assistantMessage, "assistant");
    }

    @Override
    public void onToolExecution(MemoryContextRequest request, ToolExecutionRecord toolExecution) {
        validateRequest(request);
        if (toolExecution == null) {
            return;
        }

        recordEpisode(request, episodicEventFactory.toolExecutionEvent(request, toolExecution));
        if (!toolExecution.details().isBlank()) {
            writeImplicitMemories(request, toolExecution.details(), "tool");
        }
    }

    @Override
    public void markContextMemoriesUsed(List<Long> semanticMemoryIds) {
        if (!properties.isMarkUsedEnabled() || semanticMemoryIds == null || semanticMemoryIds.isEmpty()) {
            return;
        }
        for (Long id : semanticMemoryIds) {
            if (id == null) {
                continue;
            }
            try {
                semanticMemoryService.markUsed(id);
            } catch (Exception e) {
                log.warn("Unable to mark semantic memory as used id={}", id, e);
            }
        }
    }

    private void appendConversationMessage(MemoryContextRequest request, ConversationRole role, String content) {
        ConversationMessage message = new ConversationMessage(
                UUID.randomUUID(),
                request.agentId(),
                request.conversationId(),
                role,
                content,
                Instant.now(),
                buildMetadata(request)
        );

        conversationMemoryService.appendMessage(
                new ConversationMemoryKey(request.agentId(), request.conversationId()),
                message
        );
    }

    private void writeImplicitMemories(MemoryContextRequest request, String text, String source) {
        if (properties.isEnableSemanticExtraction()) {
            implicitMemoryWritePipeline.persistSemanticExtractions(request, text, source);
        }
        if (properties.isEnableRulePromotion()) {
            implicitMemoryWritePipeline.promoteRules(request, text, source);
        }
    }

    private ContextRequest toContextRequest(MemoryContextRequest request) {
        return new ContextRequest(
                request.agentId(),
                request.userId(),
                request.conversationId(),
                request.projectId(),
                request.currentUserMessage() == null ? "(no user message)" : request.currentUserMessage(),
                request.maxSemanticMemories(),
                request.maxEpisodes()
        );
    }

    private java.util.Map<String, Object> buildMetadata(MemoryContextRequest request) {
        java.util.Map<String, Object> metadata = new java.util.HashMap<>();
        if (request.projectId() != null && !request.projectId().isBlank()) {
            metadata.put("projectId", request.projectId());
        }
        if (request.userId() != null && !request.userId().isBlank()) {
            metadata.put("userId", request.userId());
        }
        if (request.botId() != null && !request.botId().isBlank()) {
            metadata.put("botId", request.botId());
        }
        return metadata.isEmpty() ? java.util.Map.of() : java.util.Map.copyOf(metadata);
    }

    private void recordEpisode(MemoryContextRequest request, fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeEvent event) {
        try {
            episodicMemoryService.record(event);
        } catch (Exception e) {
            log.warn("Unable to write episodic event for agent={} conversation={}", request.agentId(), request.conversationId(), e);
        }
    }

    private void validateRequest(MemoryContextRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
    }
}
