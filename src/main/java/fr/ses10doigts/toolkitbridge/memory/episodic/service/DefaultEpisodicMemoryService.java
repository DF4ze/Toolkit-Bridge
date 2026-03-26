package fr.ses10doigts.toolkitbridge.memory.episodic.service;

import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeEvent;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeScope;
import fr.ses10doigts.toolkitbridge.memory.episodic.repository.EpisodeEventRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
@RequiredArgsConstructor
public class DefaultEpisodicMemoryService implements EpisodicMemoryService {

    private final EpisodeEventRepository repository;
    private final Validator validator;

    @Override
    public EpisodeEvent record(EpisodeEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("event must not be null");
        }
        normalize(event);
        validate(event);
        return repository.save(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EpisodeEvent> findRecent(String agentId, int limit) {
        requireAgent(agentId);
        int safeLimit = requireLimit(limit);
        return repository.findByAgentIdOrderByCreatedAtDesc(agentId, PageRequest.of(0, safeLimit));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EpisodeEvent> findRecentByScope(String agentId, EpisodeScope scope, int limit) {
        requireAgent(agentId);
        if (scope == null) {
            throw new IllegalArgumentException("scope must not be null");
        }
        int safeLimit = requireLimit(limit);
        return repository.findByAgentIdAndScopeOrderByCreatedAtDesc(agentId, scope, PageRequest.of(0, safeLimit));
    }

    private void requireAgent(String agentId) {
        if (agentId == null || agentId.isBlank()) {
            throw new IllegalArgumentException("agentId must not be blank");
        }
    }

    private int requireLimit(int limit) {
        if (limit <= 0) {
            throw new IllegalArgumentException("limit must be positive");
        }
        return limit;
    }

    private void normalize(EpisodeEvent event) {
        if (event.getScopeId() != null && event.getScopeId().isBlank()) {
            event.setScopeId(null);
        }
        if (event.getDetails() != null && event.getDetails().isBlank()) {
            event.setDetails(null);
        }
    }

    private void validate(EpisodeEvent event) {
        var violations = validator.validate(event);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }
}
