package fr.ses10doigts.toolkitbridge.memory.rule.service;

import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleEntry;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RulePriority;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleScope;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleStatus;
import fr.ses10doigts.toolkitbridge.memory.rule.repository.RuleEntryRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
@RequiredArgsConstructor
public class DefaultRuleService implements RuleService {

    private final RuleEntryRepository repository;
    private final Validator validator;

    @Override
    public RuleEntry create(RuleEntry entry) {
        if (entry == null) {
            throw new IllegalArgumentException("entry must not be null");
        }
        normalize(entry);
        validate(entry);
        validateScope(entry);
        return repository.save(entry);
    }

    @Override
    public RuleEntry update(Long id, RuleEntry updated) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        if (updated == null) {
            throw new IllegalArgumentException("updated must not be null");
        }

        RuleEntry existing = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("RuleEntry not found: " + id));

        existing.setAgentId(updated.getAgentId());
        existing.setScope(updated.getScope());
        existing.setScopeId(updated.getScopeId());
        existing.setTitle(updated.getTitle());
        existing.setContent(updated.getContent());
        existing.setPriority(Optional.ofNullable(updated.getPriority()).orElse(existing.getPriority()));
        existing.setStatus(Optional.ofNullable(updated.getStatus()).orElse(existing.getStatus()));

        normalize(existing);
        validate(existing);
        validateScope(existing);
        return repository.save(existing);
    }

    @Override
    public void activate(Long id) {
        setStatus(id, RuleStatus.ACTIVE);
    }

    @Override
    public void deactivate(Long id) {
        setStatus(id, RuleStatus.DISABLED);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RuleEntry> getApplicableRules(String agentId, String projectId) {
        List<RuleEntry> rules = new ArrayList<>();
        rules.addAll(repository.findByScopeAndStatus(RuleScope.GLOBAL, RuleStatus.ACTIVE));

        if (agentId != null && !agentId.isBlank()) {
            rules.addAll(repository.findByScopeAndStatusAndAgentId(RuleScope.AGENT, RuleStatus.ACTIVE, agentId));
        }

        if (projectId != null && !projectId.isBlank()) {
            rules.addAll(repository.findByScopeAndStatusAndScopeId(RuleScope.PROJECT, RuleStatus.ACTIVE, projectId));
        }

        rules.sort(ruleComparator());
        return rules;
    }

    private void setStatus(Long id, RuleStatus status) {
        if (id == null) {
            throw new IllegalArgumentException("id must not be null");
        }
        RuleEntry entry = repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("RuleEntry not found: " + id));
        entry.setStatus(status);
        repository.save(entry);
    }

    private void normalize(RuleEntry entry) {
        if (entry.getAgentId() != null && entry.getAgentId().isBlank()) {
            entry.setAgentId(null);
        }
        if (entry.getScopeId() != null && entry.getScopeId().isBlank()) {
            entry.setScopeId(null);
        }
    }

    private void validate(RuleEntry entry) {
        var violations = validator.validate(entry);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private void validateScope(RuleEntry entry) {
        RuleScope scope = entry.getScope();
        if (scope == RuleScope.AGENT) {
            if (entry.getAgentId() == null || entry.getAgentId().isBlank()) {
                throw new IllegalArgumentException("agentId must be provided for AGENT scope");
            }
        }
        if (scope == RuleScope.PROJECT) {
            if (entry.getScopeId() == null || entry.getScopeId().isBlank()) {
                throw new IllegalArgumentException("scopeId must be provided for PROJECT scope");
            }
        }
    }

    private Comparator<RuleEntry> ruleComparator() {
        return Comparator
                .comparingInt((RuleEntry entry) -> scopeWeight(entry.getScope()))
                .reversed()
                .thenComparing(Comparator.comparingInt((RuleEntry entry) -> priorityWeight(entry.getPriority()))
                        .reversed());
    }

    private int scopeWeight(RuleScope scope) {
        if (scope == null) {
            return 0;
        }
        return switch (scope) {
            case PROJECT -> 3;
            case AGENT -> 2;
            case GLOBAL -> 1;
        };
    }

    private int priorityWeight(RulePriority priority) {
        if (priority == null) {
            return 0;
        }
        return switch (priority) {
            case CRITICAL -> 4;
            case HIGH -> 3;
            case MEDIUM -> 2;
            case LOW -> 1;
        };
    }
}
