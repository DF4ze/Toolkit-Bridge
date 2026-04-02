package fr.ses10doigts.toolkitbridge.memory.retrieval.facade;

import fr.ses10doigts.toolkitbridge.memory.context.model.ContextRequest;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMemoryKey;
import fr.ses10doigts.toolkitbridge.memory.conversation.port.ConversationMemoryService;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeEvent;
import fr.ses10doigts.toolkitbridge.memory.episodic.service.EpisodicMemoryService;
import fr.ses10doigts.toolkitbridge.memory.retrieval.config.MemoryRetrievalProperties;
import fr.ses10doigts.toolkitbridge.memory.retrieval.model.MemoryQuery;
import fr.ses10doigts.toolkitbridge.memory.retrieval.model.RetrievedMemories;
import fr.ses10doigts.toolkitbridge.memory.retrieval.port.MemoryRetriever;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleEntry;
import fr.ses10doigts.toolkitbridge.memory.rule.service.RuleService;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryScope;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class DefaultMemoryRetrievalFacade implements MemoryRetrievalFacade {

    private static final Set<MemoryScope> DEFAULT_SCOPES = Set.of(
            MemoryScope.AGENT,
            MemoryScope.SHARED,
            MemoryScope.PROJECT
    );

    private static final Set<MemoryType> DEFAULT_TYPES = Set.of(
            MemoryType.FACT,
            MemoryType.CONTEXT,
            MemoryType.DECISION
    );

    private final RuleService ruleService;
    private final MemoryRetriever memoryRetriever;
    private final EpisodicMemoryService episodicMemoryService;
    private final ConversationMemoryService conversationMemoryService;
    private final MemoryRetrievalProperties properties;

    @Override
    public RetrievedMemories retrieve(ContextRequest contextRequest) {
        if (contextRequest == null) {
            throw new IllegalArgumentException("contextRequest must not be null");
        }

        List<RuleEntry> rules = ruleService.getApplicableRules(contextRequest.agentId(), contextRequest.projectId())
                .stream()
                .limit(properties.getMaxRules())
                .toList();

        List<MemoryEntry> semanticMemories = memoryRetriever.retrieve(new MemoryQuery(
                contextRequest.agentId(),
                contextRequest.projectId(),
                contextRequest.userMessage(),
                DEFAULT_SCOPES,
                DEFAULT_TYPES,
                Math.max(1, properties.getMaxSemanticMemories())
        ));

        List<RetrievedMemories.EpisodeSummary> episodicMemories = episodicMemoryService.findRecent(
                        contextRequest.agentId(),
                        Math.max(1, properties.getMaxEpisodes())
                ).stream()
                .map(this::summarizeEpisode)
                .toList();

        String conversationSlice = conversationMemoryService.buildContext(
                new ConversationMemoryKey(contextRequest.agentId(), contextRequest.conversationId())
        );

        conversationSlice = trimConversationSlice(conversationSlice, properties.getConversationSliceMaxCharacters());

        return new RetrievedMemories(rules, semanticMemories, episodicMemories, conversationSlice);
    }

    private RetrievedMemories.EpisodeSummary summarizeEpisode(EpisodeEvent event) {
        String summary = event.getAction();
        if (event.getDetails() != null && !event.getDetails().isBlank()) {
            summary = summary + " - " + event.getDetails();
        }
        Instant created = event.getCreatedAt() == null ? Instant.EPOCH : event.getCreatedAt();
        return new RetrievedMemories.EpisodeSummary(
                event.getType(),
                event.getStatus(),
                summary,
                created,
                event.getScope() == null ? null : event.getScope().name(),
                event.getScopeId()
        );
    }

    private String trimConversationSlice(String slice, int maxCharacters) {
        if (slice == null || slice.isBlank()) {
            return "";
        }
        if (maxCharacters <= 0 || slice.length() <= maxCharacters) {
            return slice.trim();
        }
        return slice.substring(slice.length() - maxCharacters).stripLeading();
    }
}
