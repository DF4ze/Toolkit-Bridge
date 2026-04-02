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
import fr.ses10doigts.toolkitbridge.memory.retrieval.model.RetrievedMemories;
import fr.ses10doigts.toolkitbridge.memory.retrieval.facade.MemoryRetrievalFacade;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleEntry;
import fr.ses10doigts.toolkitbridge.memory.rule.promotion.RulePromotionService;
import fr.ses10doigts.toolkitbridge.memory.rule.service.RuleService;
import fr.ses10doigts.toolkitbridge.memory.semantic.extractor.SemanticMemoryExtractor;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;
import fr.ses10doigts.toolkitbridge.memory.semantic.service.SemanticMemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
    private final RuleService ruleService;
    private final SemanticMemoryExtractor semanticMemoryExtractor;
    private final RulePromotionService rulePromotionService;
    private final EpisodicEventFactory episodicEventFactory;
    private final MemoryIntegrationProperties properties;

    @Override
    public MemoryContext buildContext(MemoryContextRequest request) {
        validateRequest(request);

        ContextRequest contextRequest = toContextRequest(request);
        RetrievedMemories retrieved = memoryRetrievalFacade.retrieve(contextRequest);
        RetrievedMemories filtered = enforceLimits(request, retrieved);

        AssembledContext assembled = contextAssembler.buildContext(contextRequest, filtered);
        String bounded = enforceCharacterBudget(assembled.text(), request.tokenBudgetHint());

        return new MemoryContext(bounded, assembled.injectedSemanticMemoryIds());
    }

    @Override
    public void onUserMessage(MemoryContextRequest request) {
        validateRequest(request);
        if (request.currentUserMessage() == null || request.currentUserMessage().isBlank()) {
            return;
        }

        appendConversationMessage(request, ConversationRole.USER, request.currentUserMessage());
        recordEpisode(request, episodicEventFactory.userMessageReceived(request));
        extractAndPersistSemantic(request, request.currentUserMessage(), "user");
        promoteAndPersistRules(request, request.currentUserMessage(), "user");
    }

    @Override
    public void onAssistantMessage(MemoryContextRequest request, String assistantMessage) {
        validateRequest(request);
        if (assistantMessage == null || assistantMessage.isBlank()) {
            return;
        }

        appendConversationMessage(request, ConversationRole.ASSISTANT, assistantMessage);
        recordEpisode(request, episodicEventFactory.assistantResponseGenerated(request, assistantMessage));
        extractAndPersistSemantic(request, assistantMessage, "assistant");
        promoteAndPersistRules(request, assistantMessage, "assistant");
    }

    @Override
    public void onToolExecution(MemoryContextRequest request, ToolExecutionRecord toolExecution) {
        validateRequest(request);
        if (toolExecution == null) {
            return;
        }

        recordEpisode(request, episodicEventFactory.toolExecutionEvent(request, toolExecution));
        if (!toolExecution.details().isBlank()) {
            extractAndPersistSemantic(request, toolExecution.details(), "tool");
            promoteAndPersistRules(request, toolExecution.details(), "tool");
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

    private RetrievedMemories enforceLimits(MemoryContextRequest request, RetrievedMemories input) {
        int maxRules = positive(properties.getMaxRules(), 10);
        int maxSemantic = positive(
                request.maxSemanticMemories() == null ? properties.getMaxSemanticMemories() : request.maxSemanticMemories(),
                10
        );
        int maxEpisodes = positive(
                request.maxEpisodes() == null ? properties.getMaxEpisodes() : request.maxEpisodes(),
                5
        );

        return new RetrievedMemories(
                limit(input.rules(), maxRules),
                limit(input.semanticMemories(), maxSemantic),
                properties.isEnableEpisodicInjection() ? limit(input.episodicMemories(), maxEpisodes) : List.of(),
                input.conversationSlice()
        );
    }

    private String enforceCharacterBudget(String text, Integer tokenBudgetHint) {
        int charBudget = positive(properties.getMaxContextCharacters(), 15000);
        if (tokenBudgetHint != null && tokenBudgetHint > 0) {
            charBudget = Math.min(charBudget, tokenBudgetHint * 4);
        }
        if (text == null || text.length() <= charBudget) {
            return text == null ? "" : text;
        }
        return text.substring(text.length() - charBudget);
    }

    private <T> List<T> limit(List<T> values, int max) {
        if (values == null || values.isEmpty() || max <= 0) {
            return List.of();
        }
        return values.stream().limit(max).toList();
    }

    private int positive(int value, int fallback) {
        return value > 0 ? value : fallback;
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

    private void extractAndPersistSemantic(MemoryContextRequest request, String text, String source) {
        if (!properties.isEnableSemanticExtraction()) {
            return;
        }
        for (MemoryEntry candidate : semanticMemoryExtractor.extract(request, text, source)) {
            if (alreadyExists(candidate)) {
                continue;
            }
            try {
                semanticMemoryService.create(candidate);
            } catch (Exception e) {
                log.warn("Unable to persist semantic memory for agent={} content='{}'", request.agentId(), candidate.getContent(), e);
            }
        }
    }

    private void promoteAndPersistRules(MemoryContextRequest request, String text, String source) {
        if (!properties.isEnableRulePromotion()) {
            return;
        }
        List<RuleEntry> existing = safeApplicableRules(request);
        for (RuleEntry candidate : rulePromotionService.promote(request, text, source)) {
            if (existing.stream().anyMatch(rule -> sameRule(rule, candidate))) {
                continue;
            }
            try {
                ruleService.create(candidate);
                existing.add(candidate);
            } catch (Exception e) {
                log.warn("Unable to persist promoted rule for agent={} content='{}'", request.agentId(), candidate.getContent(), e);
            }
        }
    }

    private List<RuleEntry> safeApplicableRules(MemoryContextRequest request) {
        try {
            return new ArrayList<>(ruleService.getApplicableRules(request.agentId(), request.projectId()));
        } catch (Exception e) {
            log.warn("Unable to load existing rules for agent={}", request.agentId(), e);
            return new ArrayList<>();
        }
    }

    private boolean alreadyExists(MemoryEntry candidate) {
        List<MemoryEntry> existing;
        try {
            existing = semanticMemoryService.search(candidate.getAgentId(), candidate.getContent());
        } catch (Exception e) {
            return false;
        }
        return existing.stream().anyMatch(entry -> sameMemory(entry, candidate));
    }

    private boolean sameMemory(MemoryEntry a, MemoryEntry b) {
        if (a == null || b == null) {
            return false;
        }
        return normalize(a.getContent()).equals(normalize(b.getContent()))
                && a.getScope() == b.getScope()
                && normalize(a.getScopeId()).equals(normalize(b.getScopeId()))
                && a.getType() == b.getType();
    }

    private boolean sameRule(RuleEntry a, RuleEntry b) {
        if (a == null || b == null) {
            return false;
        }
        return normalize(a.getContent()).equals(normalize(b.getContent()))
                && a.getScope() == b.getScope()
                && normalize(a.getScopeId()).equals(normalize(b.getScopeId()))
                && normalize(a.getAgentId()).equals(normalize(b.getAgentId()));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private ContextRequest toContextRequest(MemoryContextRequest request) {
        return new ContextRequest(
                request.agentId(),
                request.conversationId(),
                request.projectId(),
                request.currentUserMessage() == null ? "(no user message)" : request.currentUserMessage()
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
