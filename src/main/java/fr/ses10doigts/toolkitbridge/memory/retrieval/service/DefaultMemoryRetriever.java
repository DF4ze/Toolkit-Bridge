package fr.ses10doigts.toolkitbridge.memory.retrieval.service;

import fr.ses10doigts.toolkitbridge.memory.retrieval.model.MemoryQuery;
import fr.ses10doigts.toolkitbridge.memory.retrieval.port.MemoryRetriever;
import fr.ses10doigts.toolkitbridge.memory.scoring.service.MemoryScoringService;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryStatus;
import fr.ses10doigts.toolkitbridge.memory.semantic.repository.MemoryEntryRepository;
import fr.ses10doigts.toolkitbridge.memory.semantic.scope.MemoryScopePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DefaultMemoryRetriever implements MemoryRetriever {

    private final MemoryEntryRepository repository;
    private final MemoryScoringService scoringService;
    private final MemoryScopePolicy scopePolicy;

    @Override
    public List<MemoryEntry> retrieve(MemoryQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("query must not be null");
        }

        List<MemoryEntry> candidates = repository.searchCandidates(
                query.agentId(),
                MemoryStatus.ACTIVE,
                query.textQuery()
        );

        return candidates.stream()
                .filter(entry -> entry.getStatus() == MemoryStatus.ACTIVE)
                .filter(entry -> query.scopes().isEmpty() || query.scopes().contains(entry.getScope()))
                .filter(entry -> scopePolicy.isEntryVisible(entry, query.userId(), query.projectId()))
                .filter(entry -> query.types().isEmpty() || query.types().contains(entry.getType()))
                .sorted(Comparator.comparingDouble(scoringService::computeScore).reversed())
                .limit(query.limit())
                .toList();
    }
}
