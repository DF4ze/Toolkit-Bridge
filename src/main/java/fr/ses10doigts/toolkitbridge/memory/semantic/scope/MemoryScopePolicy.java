package fr.ses10doigts.toolkitbridge.memory.semantic.scope;

import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryScope;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class MemoryScopePolicy {

    public MemoryScope resolveDurableWriteScope(String userId, String projectId) {
        if (hasText(projectId)) {
            return MemoryScope.PROJECT;
        }
        if (hasText(userId)) {
            return MemoryScope.USER;
        }
        return MemoryScope.AGENT;
    }

    public String resolveScopeId(MemoryScope scope, String userId, String projectId) {
        if (scope == null) {
            return null;
        }
        return switch (scope) {
            case USER -> normalize(userId);
            case PROJECT -> normalize(projectId);
            default -> null;
        };
    }

    public Set<MemoryScope> defaultReadableScopes() {
        LinkedHashSet<MemoryScope> scopes = new LinkedHashSet<>();
        scopes.add(MemoryScope.SYSTEM);
        scopes.add(MemoryScope.AGENT);
        scopes.add(MemoryScope.USER);
        scopes.add(MemoryScope.PROJECT);
        scopes.add(MemoryScope.SHARED);
        return Set.copyOf(scopes);
    }

    public boolean isEntryVisible(MemoryEntry entry, String userId, String projectId) {
        if (entry == null || entry.getScope() == null) {
            return false;
        }
        return switch (entry.getScope()) {
            case SYSTEM, AGENT, SHARED -> true;
            case USER -> sameId(entry.getScopeId(), userId);
            case PROJECT -> sameId(entry.getScopeId(), projectId);
        };
    }

    private boolean sameId(String a, String b) {
        String left = normalize(a);
        String right = normalize(b);
        return left != null && left.equals(right);
    }

    private boolean hasText(String value) {
        return normalize(value) != null;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
