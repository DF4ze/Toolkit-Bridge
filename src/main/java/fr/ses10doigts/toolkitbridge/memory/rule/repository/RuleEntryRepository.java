package fr.ses10doigts.toolkitbridge.memory.rule.repository;

import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleEntry;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleScope;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RuleEntryRepository extends JpaRepository<RuleEntry, Long> {

    List<RuleEntry> findByScopeAndStatus(RuleScope scope, RuleStatus status);

    List<RuleEntry> findByScopeAndStatusAndAgentId(RuleScope scope, RuleStatus status, String agentId);

    List<RuleEntry> findByScopeAndStatusAndScopeId(RuleScope scope, RuleStatus status, String scopeId);
}
