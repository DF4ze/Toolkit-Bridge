package fr.ses10doigts.toolkitbridge.memory.retrieval.model;

import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeEventType;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeStatus;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleEntry;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;

import java.time.Instant;
import java.util.List;

public record RetrievedMemories(
        List<RuleEntry> rules,
        List<MemoryEntry> semanticMemories,
        List<EpisodeSummary> episodicMemories,
        String conversationSlice
) {

    public record EpisodeSummary(
            EpisodeEventType type,
            EpisodeStatus status,
            String summary,
            Instant createdAt,
            String scope,
            String scopeId
    ) {
    }
}
