package fr.ses10doigts.toolkitbridge.memory.semantic.extractor;

import fr.ses10doigts.toolkitbridge.memory.facade.model.MemoryContextRequest;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryScope;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryType;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class DefaultSemanticMemoryExtractor implements SemanticMemoryExtractor {

    private static final Set<String> DURABLE_MARKERS = Set.of(
            "prefere", "prefer", "always use", "utilise", "use ", "convention",
            "architecture", "stack", "noms de classes", "class names", "must ", "doit"
    );

    private static final Set<String> EPHEMERAL_MARKERS = Set.of(
            "aujourd", "today", "demain", "tomorrow", "cette semaine", "this week",
            "fais-moi", "peux-tu", "can you", "please"
    );

    @Override
    public List<MemoryEntry> extract(MemoryContextRequest request, String text, String source) {
        if (request == null || text == null || text.isBlank()) {
            return List.of();
        }

        List<String> candidates = splitSentences(text);
        List<MemoryEntry> extracted = new ArrayList<>();
        Set<String> dedupe = new LinkedHashSet<>();

        for (String candidate : candidates) {
            String normalized = candidate.toLowerCase(Locale.ROOT);
            if (!isDurableCandidate(normalized) || isEphemeral(normalized)) {
                continue;
            }
            String normalizedContent = candidate.trim();
            if (!dedupe.add(normalizedContent.toLowerCase(Locale.ROOT))) {
                continue;
            }

            MemoryEntry entry = new MemoryEntry();
            entry.setAgentId(request.agentId());
            entry.setScope(request.projectId() == null ? MemoryScope.AGENT : MemoryScope.PROJECT);
            entry.setScopeId(request.projectId());
            entry.setType(resolveType(normalized));
            entry.setContent(normalizedContent);
            entry.setImportance(resolveImportance(normalized));
            entry.setTags(Set.of("extracted", source == null ? "unknown" : source));
            extracted.add(entry);
        }

        return extracted;
    }

    private List<String> splitSentences(String text) {
        String[] parts = text.split("[\\n.!?]+");
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            String trimmed = part == null ? "" : part.trim();
            if (!trimmed.isBlank()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private boolean isDurableCandidate(String text) {
        return DURABLE_MARKERS.stream().anyMatch(text::contains);
    }

    private boolean isEphemeral(String text) {
        return EPHEMERAL_MARKERS.stream().anyMatch(text::contains);
    }

    private MemoryType resolveType(String text) {
        if (text.contains("prefere") || text.contains("prefer")) {
            return MemoryType.PREFERENCE;
        }
        if (text.contains("decision") || text.contains("choix") || text.contains("doit")) {
            return MemoryType.DECISION;
        }
        return MemoryType.CONTEXT;
    }

    private double resolveImportance(String text) {
        if (text.contains("always") || text.contains("toujours") || text.contains("must") || text.contains("doit")) {
            return 0.85;
        }
        if (text.contains("prefere") || text.contains("prefer")) {
            return 0.75;
        }
        return 0.65;
    }
}