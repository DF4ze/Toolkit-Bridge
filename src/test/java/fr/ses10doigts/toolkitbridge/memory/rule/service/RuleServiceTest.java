package fr.ses10doigts.toolkitbridge.memory.rule.service;

import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleEntry;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RulePriority;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleScope;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleStatus;
import fr.ses10doigts.toolkitbridge.memory.rule.repository.RuleEntryRepository;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class RuleServiceTest {

    private static Validator validator;
    private RuleEntryRepository repository;
    private RuleService service;

    @BeforeAll
    static void beforeAll() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @BeforeEach
    void setUp() {
        repository = mock(RuleEntryRepository.class);
        service = new DefaultRuleService(repository, validator);
    }

    @Test
    void createValidRule() {
        RuleEntry entry = new RuleEntry();
        entry.setScope(RuleScope.AGENT);
        entry.setAgentId("agent-1");
        entry.setTitle("Use configuration properties");
        entry.setContent("Always use @ConfigurationProperties");
        entry.setPriority(RulePriority.HIGH);
        entry.setStatus(RuleStatus.ACTIVE);

        when(repository.save(any(RuleEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RuleEntry saved = service.create(entry);

        assertThat(saved.getAgentId()).isEqualTo("agent-1");
        verify(repository).save(any(RuleEntry.class));
    }

    @Test
    void rejectInvalidRule() {
        RuleEntry entry = new RuleEntry();
        entry.setScope(RuleScope.GLOBAL);
        entry.setTitle(" ");
        entry.setContent("");

        assertThatThrownBy(() -> service.create(entry))
                .isInstanceOf(ConstraintViolationException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void deactivateRule() {
        RuleEntry existing = new RuleEntry();
        existing.setId(2L);
        existing.setScope(RuleScope.GLOBAL);
        existing.setTitle("Title");
        existing.setContent("Content");
        existing.setPriority(RulePriority.MEDIUM);
        existing.setStatus(RuleStatus.ACTIVE);

        when(repository.findById(2L)).thenReturn(java.util.Optional.of(existing));
        when(repository.save(any(RuleEntry.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.deactivate(2L);

        ArgumentCaptor<RuleEntry> captor = ArgumentCaptor.forClass(RuleEntry.class);
        verify(repository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(RuleStatus.DISABLED);
    }

    @Test
    void getApplicableRulesRespectsScopeAndPriority() {
        RuleEntry globalLow = rule(RuleScope.GLOBAL, RulePriority.LOW, null, null);
        RuleEntry agentHigh = rule(RuleScope.AGENT, RulePriority.HIGH, "agent-1", null);
        RuleEntry projectCritical = rule(RuleScope.PROJECT, RulePriority.CRITICAL, null, "project-1");

        when(repository.findByScopeAndStatus(RuleScope.GLOBAL, RuleStatus.ACTIVE))
                .thenReturn(List.of(globalLow));
        when(repository.findByScopeAndStatusAndAgentId(RuleScope.AGENT, RuleStatus.ACTIVE, "agent-1"))
                .thenReturn(List.of(agentHigh));
        when(repository.findByScopeAndStatusAndScopeId(RuleScope.PROJECT, RuleStatus.ACTIVE, "project-1"))
                .thenReturn(List.of(projectCritical));

        List<RuleEntry> result = service.getApplicableRules("agent-1", "project-1");

        assertThat(result).containsExactly(projectCritical, agentHigh, globalLow);
    }

    @Test
    void rejectsAgentScopeWithoutAgentId() {
        RuleEntry entry = new RuleEntry();
        entry.setScope(RuleScope.AGENT);
        entry.setTitle("Title");
        entry.setContent("Content");
        entry.setPriority(RulePriority.MEDIUM);
        entry.setStatus(RuleStatus.ACTIVE);

        assertThatThrownBy(() -> service.create(entry))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("agentId");
    }

    @Test
    void rejectsProjectScopeWithoutScopeId() {
        RuleEntry entry = new RuleEntry();
        entry.setScope(RuleScope.PROJECT);
        entry.setTitle("Title");
        entry.setContent("Content");
        entry.setPriority(RulePriority.MEDIUM);
        entry.setStatus(RuleStatus.ACTIVE);

        assertThatThrownBy(() -> service.create(entry))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("scopeId");
    }

    private RuleEntry rule(RuleScope scope, RulePriority priority, String agentId, String scopeId) {
        RuleEntry entry = new RuleEntry();
        entry.setScope(scope);
        entry.setPriority(priority);
        entry.setStatus(RuleStatus.ACTIVE);
        entry.setTitle("title");
        entry.setContent("content");
        entry.setAgentId(agentId);
        entry.setScopeId(scopeId);
        return entry;
    }
}
