package fr.ses10doigts.toolkitbridge.memory.tool.model;

import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryScope;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryType;

import java.util.Set;

public record ExplicitFactMemoryWriteRequest(
        Long memoryId,
        String agentId,
        String userId,
        String projectId,
        MemoryScope scope,
        String scopeId,
        MemoryType type,
        String content,
        Double importance,
        Set<String> tags
) {
}
