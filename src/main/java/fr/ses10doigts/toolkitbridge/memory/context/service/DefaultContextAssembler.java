package fr.ses10doigts.toolkitbridge.memory.context.service;

import fr.ses10doigts.toolkitbridge.memory.context.config.ContextAssemblerProperties;
import fr.ses10doigts.toolkitbridge.memory.context.model.ContextRequest;
import fr.ses10doigts.toolkitbridge.memory.context.port.ContextAssembler;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMemoryKey;
import fr.ses10doigts.toolkitbridge.memory.conversation.port.ConversationMemoryService;
import fr.ses10doigts.toolkitbridge.memory.retrieval.model.MemoryQuery;
import fr.ses10doigts.toolkitbridge.memory.retrieval.port.MemoryRetriever;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleEntry;
import fr.ses10doigts.toolkitbridge.memory.rule.service.RuleService;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryScope;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryType;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;

@Service
public class DefaultContextAssembler implements ContextAssembler {

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
    private final ConversationMemoryService conversationMemoryService;
    private final MemoryRetriever memoryRetriever;
    private final ContextAssemblerProperties properties;

    public DefaultContextAssembler(
            RuleService ruleService,
            ConversationMemoryService conversationMemoryService,
            MemoryRetriever memoryRetriever,
            ContextAssemblerProperties properties
    ) {
        this.ruleService = ruleService;
        this.conversationMemoryService = conversationMemoryService;
        this.memoryRetriever = memoryRetriever;
        this.properties = properties;
        validateProperties(properties);
    }

    @Override
    public String buildContext(ContextRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }

        int maxMemories = resolveLimit(request.maxSemanticMemories(), properties.getMaxMemories());
        StringBuilder context = new StringBuilder();

        List<RuleEntry> rules = ruleService.getApplicableRules(request.agentId(), request.projectId())
                .stream()
                .limit(properties.getMaxRules())
                .toList();

        context.append("## Rules\n");
        for (RuleEntry rule : rules) {
            context.append("- [")
                    .append(rule.getPriority())
                    .append("] ")
                    .append(rule.getContent())
                    .append("\n");
        }

        List<MemoryEntry> memories = memoryRetriever.retrieve(new MemoryQuery(
                request.agentId(),
                request.projectId(),
                request.currentUserMessage(),
                DEFAULT_SCOPES,
                DEFAULT_TYPES,
                maxMemories
        )).stream()
                .limit(maxMemories)
                .toList();

        context.append("\n## Relevant Memories\n");
        for (MemoryEntry memory : memories) {
            context.append("- ")
                    .append(memory.getContent())
                    .append("\n");
        }

        String conversation = "";
        if (request.conversationId() != null && !request.conversationId().isBlank()) {
            conversation = conversationMemoryService.buildContext(
                    new ConversationMemoryKey(request.agentId(), request.conversationId())
            );
        }

        context.append("\n## Conversation\n");
        if (conversation != null && !conversation.isBlank()) {
            context.append(conversation.trim());
        }

        context.append("\n\n## User Input\n");
        context.append(request.currentUserMessage());

        return trim(context.toString());
    }

    private String trim(String context) {
        int maxCharacters = properties.getMaxCharacters();
        if (context.length() <= maxCharacters) {
            return context;
        }
        return context.substring(context.length() - maxCharacters);
    }

    private void validateProperties(ContextAssemblerProperties props) {
        if (props.getMaxRules() <= 0) {
            throw new IllegalStateException("maxRules must be > 0");
        }
        if (props.getMaxMemories() <= 0) {
            throw new IllegalStateException("maxMemories must be > 0");
        }
        if (props.getMaxCharacters() <= 0) {
            throw new IllegalStateException("maxCharacters must be > 0");
        }
    }

    private int resolveLimit(Integer override, int fallback) {
        if (override == null) {
            return fallback;
        }
        return Math.min(override, fallback);
    }
}
