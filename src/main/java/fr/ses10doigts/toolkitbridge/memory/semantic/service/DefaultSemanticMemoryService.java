package fr.ses10doigts.toolkitbridge.memory.semantic.service;

import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryScope;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryStatus;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryType;
import fr.ses10doigts.toolkitbridge.memory.semantic.repository.MemoryEntryRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
@Transactional
public class DefaultSemanticMemoryService implements SemanticMemoryService {

    private final MemoryEntryRepository repository;
    private final Validator validator;

    public DefaultSemanticMemoryService(MemoryEntryRepository repository, Validator validator) {
        this.repository = repository;
        this.validator = validator;
    }

    @Override
    public MemoryEntry create(MemoryEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("entry must not be null");
        }
        normalize(entry);
        validate(entry);
        return repository.save(entry);
    }

    @Override
    public MemoryEntry update(Long id, MemoryEntry updated) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (updated == null) {
            throw new IllegalArgumentException("updated must not be null");
        }

        MemoryEntry existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("MemoryEntry not found: " + id));

        existing.setAgentId(updated.getAgentId());
        existing.setScope(updated.getScope());
        existing.setScopeId(updated.getScopeId());
        existing.setType(updated.getType());
        existing.setContent(updated.getContent());
        existing.setImportance(updated.getImportance());
        existing.setStatus(Optional.ofNullable(updated.getStatus()).orElse(existing.getStatus()));

        Set<String> tags = updated.getTags() == null ? new HashSet<>() : new HashSet<>(updated.getTags());
        existing.setTags(tags);

        normalize(existing);
        validate(existing);
        return repository.save(existing);
    }

    @Override
    public void archive(Long id) {
        setStatus(id, MemoryStatus.ARCHIVED);
    }

    @Override
    public void markObsolete(Long id) {
        setStatus(id, MemoryStatus.OBSOLETE);
    }

    @Override
    public void markUsed(Long id) {
        MemoryEntry entry = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("MemoryEntry not found: " + id));
        entry.setUsageCount(entry.getUsageCount() + 1);
        entry.setLastAccessedAt(Instant.now());
        repository.save(entry);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemoryEntry> findByScope(String agentId, MemoryScope scope) {
        requireAgent(agentId);
        if (scope == null) {
            throw new IllegalArgumentException("scope must not be null");
        }
        return repository.findByAgentIdAndScope(agentId, scope);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemoryEntry> findByType(String agentId, MemoryType type) {
        requireAgent(agentId);
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        return repository.findByAgentIdAndType(agentId, type);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemoryEntry> search(String agentId, String query) {
        requireAgent(agentId);
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("query must not be blank");
        }
        return repository.searchByAgentIdAndContent(agentId, query.trim());
    }

    @Override
    @Transactional(readOnly = true)
    public List<MemoryEntry> findActiveByAgent(String agentId) {
        requireAgent(agentId);
        return repository.findByAgentIdAndStatus(agentId, MemoryStatus.ACTIVE);
    }

    private void setStatus(Long id, MemoryStatus status) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        MemoryEntry entry = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("MemoryEntry not found: " + id));
        entry.setStatus(status);
        repository.save(entry);
    }

    private void requireAgent(String agentId) {
        if (agentId == null || agentId.isBlank()) {
            throw new IllegalArgumentException("agentId must not be blank");
        }
    }

    private void normalize(MemoryEntry entry) {
        if (entry.getTags() == null) {
            entry.setTags(new HashSet<>());
        }
        if (entry.getScopeId() != null && entry.getScopeId().isBlank()) {
            entry.setScopeId(null);
        }
    }

    private void validate(MemoryEntry entry) {
        var violations = validator.validate(entry);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}
