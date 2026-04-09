package fr.ses10doigts.toolkitbridge.memory.retrieval.service;

import fr.ses10doigts.toolkitbridge.memory.config.runtime.MemoryRuntimeConfigurationResolver;
import fr.ses10doigts.toolkitbridge.memory.retrieval.model.MemoryQuery;
import fr.ses10doigts.toolkitbridge.memory.retrieval.port.MemoryRetriever;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryStatus;
import fr.ses10doigts.toolkitbridge.memory.semantic.repository.MemoryEntryRepository;
import fr.ses10doigts.toolkitbridge.memory.semantic.scope.MemoryScopePolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DefaultMemoryRetriever implements MemoryRetriever {

    private static final Sort STABLE_CANDIDATE_ORDER = Sort.by(
            Sort.Order.desc("updatedAt"),
            Sort.Order.desc("createdAt"),
            Sort.Order.desc("id")
    );

    private final MemoryEntryRepository repository;
    private final MemoryRuntimeConfigurationResolver runtimeConfigurationResolver;
    private final MemoryScopePolicy scopePolicy;

    @Override
    public List<MemoryEntry> retrieve(MemoryQuery query) {
        if (query == null) {
            throw new IllegalArgumentException("query must not be null");
        }

        List<MemoryEntry> candidates = repository.searchCandidates(
                query.agentId(),
                MemoryStatus.ACTIVE,
                query.textQuery(),
                PageRequest.of(0, resolveCandidateFetchLimit(query), STABLE_CANDIDATE_ORDER)
        );

        return candidates.stream()
                .filter(entry -> entry.getStatus() == MemoryStatus.ACTIVE)
                .filter(entry -> query.scopes().isEmpty() || query.scopes().contains(entry.getScope()))
                .filter(entry -> scopePolicy.isEntryVisible(entry, query.userId(), query.projectId()))
                .filter(entry -> query.types().isEmpty() || query.types().contains(entry.getType()))
                .limit(query.candidateLimit())
                .toList();
    }

    private int resolveCandidateFetchLimit(MemoryQuery query) {
        int configuredLimit = Math.max(1, runtimeConfigurationResolver.snapshot().retrieval().maxCandidatePoolSize());
        return Math.min(query.candidateLimit(), configuredLimit);
    }
}
