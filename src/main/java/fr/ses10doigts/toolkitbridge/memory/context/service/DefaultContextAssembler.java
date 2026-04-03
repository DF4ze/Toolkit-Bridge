package fr.ses10doigts.toolkitbridge.memory.context.service;

import fr.ses10doigts.toolkitbridge.memory.context.config.ContextAssemblerProperties;
import fr.ses10doigts.toolkitbridge.memory.context.model.AssembledContext;
import fr.ses10doigts.toolkitbridge.memory.context.model.ContextRequest;
import fr.ses10doigts.toolkitbridge.memory.context.port.ContextAssembler;
import fr.ses10doigts.toolkitbridge.memory.retrieval.model.RetrievedMemories;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleEntry;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class DefaultContextAssembler implements ContextAssembler {

    private final ContextAssemblerProperties properties;

    public DefaultContextAssembler(
            ContextAssemblerProperties properties
    ) {
        this.properties = properties;
        validateProperties(properties);
    }

    @Override
    public AssembledContext buildContext(ContextRequest request, RetrievedMemories retrievedMemories) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }
        if (retrievedMemories == null) {
            throw new IllegalStateException("retrievedMemories must not be null");
        }

        StringBuilder context = new StringBuilder();
        LinkedHashSet<Long> usedSemanticMemoryIds = new LinkedHashSet<>();

        appendRules(context, limit(retrievedMemories.rules(), properties.getMaxRules()));

        appendKnownFacts(
                context,
                limit(
                        retrievedMemories.semanticMemories(),
                        resolveLimit(request.maxSemanticMemories(), properties.getMaxMemories())
                ),
                usedSemanticMemoryIds
        );

        appendEpisodes(
                context,
                limit(
                        retrievedMemories.episodicMemories(),
                        resolveLimit(request.maxEpisodes(), properties.getMaxEpisodes())
                )
        );

        String conversation = retrievedMemories.conversationSlice();

        context.append("\n## Conversation\n");
        if (conversation != null && !conversation.isBlank()) {
            context.append(conversation.trim());
        }

        context.append("\n\n## User Input\n");
        context.append(request.currentUserMessage());

        return new AssembledContext(trim(context.toString()), List.copyOf(usedSemanticMemoryIds));
    }

    private void appendRules(StringBuilder context, List<RuleEntry> rules) {
        context.append("## Rules\n");
        for (RuleEntry rule : rules) {
            context.append("- [")
                    .append(rule.getPriority())
                    .append("] ")
                    .append(rule.getContent())
                    .append("\n");
        }
    }

    private void appendKnownFacts(StringBuilder context,
                                  List<MemoryEntry> memories,
                                  Set<Long> usedSemanticMemoryIds) {
        context.append("\n## Known Facts\n");
        Set<String> uniqueFacts = new LinkedHashSet<>();
        for (MemoryEntry memory : memories) {
            if (memory == null || memory.getContent() == null || memory.getContent().isBlank()) {
                continue;
            }
            if (uniqueFacts.add(memory.getContent().trim()) && memory.getId() != null) {
                usedSemanticMemoryIds.add(memory.getId());
            }
        }
        for (String fact : uniqueFacts) {
            context.append("- ")
                    .append(fact)
                    .append("\n");
        }
    }

    private void appendEpisodes(StringBuilder context, List<RetrievedMemories.EpisodeSummary> episodes) {
        if (episodes.isEmpty()) {
            return;
        }
        context.append("\n## Recent Episodes\n");
        for (RetrievedMemories.EpisodeSummary episode : episodes) {
            context.append("- [")
                    .append(episode.status())
                    .append("] ")
                    .append(episode.summary());
            context.append(" (").append(episode.type()).append(" @ ").append(formatInstant(episode.createdAt())).append(")");
            if (episode.scope() != null) {
                context.append(" scope=").append(episode.scope());
                if (episode.scopeId() != null) {
                    context.append(":").append(episode.scopeId());
                }
            }
            context.append("\n");
        }
    }

    private String formatInstant(Instant instant) {
        return instant == null ? "unknown" : instant.toString();
    }

    private String trim(String context) {
        int maxCharacters = properties.getMaxCharacters();
        if (context.length() <= maxCharacters) {
            return context;
        }
        return context.substring(context.length() - maxCharacters);
    }

    private <T> List<T> limit(List<T> values, int limit) {
        if (values == null) {
            return List.of();
        }
        if (limit <= 0) {
            return List.of();
        }
        return values.stream().limit(limit).toList();
    }

    private int resolveLimit(Integer override, int fallback) {
        if (override == null) {
            return fallback;
        }
        return Math.min(override, fallback);
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
        if (props.getMaxEpisodes() <= 0) {
            throw new IllegalStateException("maxEpisodes must be > 0");
        }
    }
}
