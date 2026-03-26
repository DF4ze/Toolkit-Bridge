package fr.ses10doigts.toolkitbridge.memory.semantic.service;

import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryScope;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryType;

import java.util.List;

public interface SemanticMemoryService {

    MemoryEntry create(MemoryEntry entry);

    MemoryEntry update(Long id, MemoryEntry updated);

    void archive(Long id);

    void markObsolete(Long id);

    void markUsed(Long id);

    List<MemoryEntry> findByScope(String agentId, MemoryScope scope);

    List<MemoryEntry> findByType(String agentId, MemoryType type);

    List<MemoryEntry> search(String agentId, String query);

    List<MemoryEntry> findActiveByAgent(String agentId);
}
