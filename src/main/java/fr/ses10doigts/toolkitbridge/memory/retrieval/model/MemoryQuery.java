package fr.ses10doigts.toolkitbridge.memory.retrieval.model;

import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryScope;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryType;

import java.util.Set;

public record MemoryQuery(
        String agentId,
        String projectId,
        String textQuery,
        Set<MemoryScope> scopes,
        Set<MemoryType> types,
        int limit
) {
    public MemoryQuery {
        if (agentId == null || agentId.isBlank()) {
            throw new IllegalArgumentException("agentId must not be blank");
        }
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be greater than 0");
        }
        if (projectId != null && projectId.isBlank()) {
            projectId = null;
        }
        scopes = scopes == null ? Set.of() : Set.copyOf(scopes);
        types = types == null ? Set.of() : Set.copyOf(types);
        if (textQuery != null && textQuery.isBlank()) {
            textQuery = null;
        }
    }
}
