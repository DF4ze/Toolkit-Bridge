package fr.ses10doigts.toolkitbridge.memory.context.service;

import fr.ses10doigts.toolkitbridge.memory.context.config.ContextAssemblerProperties;
import fr.ses10doigts.toolkitbridge.memory.context.model.AssembledContext;
import fr.ses10doigts.toolkitbridge.memory.context.model.ContextRequest;
import fr.ses10doigts.toolkitbridge.memory.context.port.ContextAssembler;
import fr.ses10doigts.toolkitbridge.memory.retrieval.model.RetrievedMemories;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleEntry;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryScope;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

        LinkedHashSet<Long> usedSemanticMemoryIds = new LinkedHashSet<>();
        StringBuilder rulesSection = new StringBuilder();
        StringBuilder factsSection = new StringBuilder();
        StringBuilder episodesSection = new StringBuilder();
        StringBuilder conversationSection = new StringBuilder();

        appendRules(rulesSection, limit(retrievedMemories.rules(), properties.getMaxRules()));
        appendFacts(factsSection, request, retrievedMemories, usedSemanticMemoryIds);

        appendEpisodes(
                episodesSection,
                limit(
                        retrievedMemories.episodicMemories(),
                        resolveLimit(request.maxEpisodes(), properties.getMaxEpisodes())
                )
        );

        String conversation = retrievedMemories.conversationSlice();
        conversationSection.append("\n## Conversation\n");
        if (conversation != null && !conversation.isBlank()) {
            conversationSection.append(conversation.trim());
        }

        String userInputSection = "\n\n## User Input\n" + request.currentUserMessage();
        String context = assembleWithPriority(
                rulesSection.toString(),
                factsSection.toString(),
                episodesSection.toString(),
                conversationSection.toString(),
                userInputSection
        );

        return new AssembledContext(context, List.copyOf(usedSemanticMemoryIds));
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

    private void appendFacts(StringBuilder context,
                             ContextRequest request,
                             RetrievedMemories retrievedMemories,
                             Set<Long> usedSemanticMemoryIds) {
        List<MemoryEntry> limitedMemories = limit(
                retrievedMemories.semanticMemories(),
                resolveLimit(request.maxSemanticMemories(), properties.getMaxMemories())
        );

        Map<MemoryScope, List<String>> factsByScope = new EnumMap<>(MemoryScope.class);
        Set<String> dedupe = new LinkedHashSet<>();
        for (MemoryEntry memory : limitedMemories) {
            if (memory == null || memory.getContent() == null || memory.getContent().isBlank()) {
                continue;
            }
            String fact = memory.getContent().trim();
            String dedupeKey = fact.toLowerCase(Locale.ROOT);
            if (!dedupe.add(dedupeKey)) {
                continue;
            }
            if (memory.getId() != null) {
                usedSemanticMemoryIds.add(memory.getId());
            }
            MemoryScope scope = normalizeScope(memory.getScope());
            factsByScope.computeIfAbsent(scope, ignored -> new java.util.ArrayList<>()).add(fact);
        }

        if (factsByScope.isEmpty()) {
            return;
        }

        context.append("\n## Facts\n");
        LinkedHashMap<String, MemoryScope> orderedSections = new LinkedHashMap<>();
        orderedSections.put("Global Context", MemoryScope.SYSTEM);
        orderedSections.put("Agent Context", MemoryScope.AGENT);
        orderedSections.put("User Context", MemoryScope.USER);
        orderedSections.put("Project Context", MemoryScope.PROJECT);

        for (Map.Entry<String, MemoryScope> section : orderedSections.entrySet()) {
            List<String> facts = factsByScope.getOrDefault(section.getValue(), List.of());
            if (facts.isEmpty()) {
                continue;
            }
            context.append("\n### ").append(section.getKey()).append("\n");
            for (String fact : facts) {
                context.append("- ").append(fact).append("\n");
            }
        }
    }

    private MemoryScope normalizeScope(MemoryScope scope) {
        if (scope == null) {
            return MemoryScope.AGENT;
        }
        if (scope == MemoryScope.SHARED) {
            return MemoryScope.SYSTEM;
        }
        return scope;
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

    private String trimToMaxCharacters(String section) {
        int maxCharacters = properties.getMaxCharacters();
        if (section.length() <= maxCharacters) {
            return section;
        }
        return section.substring(0, maxCharacters);
    }

    private String trimToLength(String section, int maxLength) {
        if (section == null || section.isEmpty() || maxLength <= 0) {
            return "";
        }
        if (section.length() <= maxLength) {
            return section;
        }
        return section.substring(0, maxLength);
    }

    private String assembleWithPriority(String rulesSection,
                                        String factsSection,
                                        String episodesSection,
                                        String conversationSection,
                                        String userInputSection) {
        int maxCharacters = properties.getMaxCharacters();
        String requiredUserSection = trimToLength(userInputSection, maxCharacters);
        StringBuilder result = new StringBuilder();

        List<String> prioritizedSections = List.of(rulesSection, factsSection, episodesSection, conversationSection);
        for (String section : prioritizedSections) {
            int remainingBeforeUser = maxCharacters - result.length() - requiredUserSection.length();
            if (remainingBeforeUser <= 0) {
                break;
            }
            result.append(trimToLength(section, remainingBeforeUser));
        }

        int remainingForUser = maxCharacters - result.length();
        result.append(trimToLength(requiredUserSection, remainingForUser));
        return trimToMaxCharacters(result.toString());
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
