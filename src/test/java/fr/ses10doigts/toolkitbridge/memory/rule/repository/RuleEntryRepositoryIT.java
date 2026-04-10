package fr.ses10doigts.toolkitbridge.memory.rule.repository;

import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleEntry;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RulePriority;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleScope;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:file:./target/test-db-${random.uuid}.db"
})
@Transactional
class RuleEntryRepositoryIT {

    @Autowired
    private RuleEntryRepository repository;

    @Test
    void findActiveByScope() {
        RuleEntry activeGlobal = createRule(RuleScope.GLOBAL, RulePriority.HIGH, null, null, RuleStatus.ACTIVE);
        RuleEntry disabledGlobal = createRule(RuleScope.GLOBAL, RulePriority.HIGH, null, null, RuleStatus.DISABLED);

        repository.save(activeGlobal);
        repository.save(disabledGlobal);

        List<RuleEntry> result = repository.findByScopeAndStatus(RuleScope.GLOBAL, RuleStatus.ACTIVE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getStatus()).isEqualTo(RuleStatus.ACTIVE);
    }

    @Test
    void findActiveByAgentScope() {
        RuleEntry forAgent = createRule(RuleScope.AGENT, RulePriority.MEDIUM, "agent-1", null, RuleStatus.ACTIVE);
        RuleEntry forOther = createRule(RuleScope.AGENT, RulePriority.MEDIUM, "agent-2", null, RuleStatus.ACTIVE);

        repository.save(forAgent);
        repository.save(forOther);

        List<RuleEntry> result = repository.findByScopeAndStatusAndAgentId(RuleScope.AGENT, RuleStatus.ACTIVE, "agent-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAgentId()).isEqualTo("agent-1");
    }

    @Test
    void findActiveByProjectScope() {
        RuleEntry forProject = createRule(RuleScope.PROJECT, RulePriority.CRITICAL, null, "project-1", RuleStatus.ACTIVE);
        RuleEntry forOther = createRule(RuleScope.PROJECT, RulePriority.CRITICAL, null, "project-2", RuleStatus.ACTIVE);

        repository.save(forProject);
        repository.save(forOther);

        List<RuleEntry> result = repository.findByScopeAndStatusAndScopeId(RuleScope.PROJECT, RuleStatus.ACTIVE, "project-1");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getScopeId()).isEqualTo("project-1");
    }

    private RuleEntry createRule(RuleScope scope, RulePriority priority, String agentId, String scopeId, RuleStatus status) {
        RuleEntry entry = new RuleEntry();
        entry.setScope(scope);
        entry.setPriority(priority);
        entry.setStatus(status);
        entry.setTitle("title");
        entry.setContent("content");
        entry.setAgentId(agentId);
        entry.setScopeId(scopeId);
        return entry;
    }
}
