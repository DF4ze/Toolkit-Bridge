package fr.ses10doigts.toolkitbridge.service.tool.memory;

import fr.ses10doigts.toolkitbridge.memory.facade.model.MemoryContext;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleEntry;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RulePriority;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleScope;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleStatus;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryScope;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryStatus;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryType;
import fr.ses10doigts.toolkitbridge.memory.shared.model.MemoryWriteMode;
import fr.ses10doigts.toolkitbridge.memory.tool.model.ExplicitFactMemoryWriteRequest;
import fr.ses10doigts.toolkitbridge.memory.tool.model.ExplicitRuleMemoryWriteRequest;
import fr.ses10doigts.toolkitbridge.memory.tool.model.MemoryContextRecallRequest;
import fr.ses10doigts.toolkitbridge.memory.tool.service.ExplicitMemoryToolService;
import fr.ses10doigts.toolkitbridge.model.dto.auth.AuthenticatedAgent;
import fr.ses10doigts.toolkitbridge.model.dto.tool.ToolExecutionResult;
import fr.ses10doigts.toolkitbridge.service.auth.CurrentAgentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemoryToolHandlerTest {

    private ExplicitMemoryToolService memoryToolService;
    private CurrentAgentService currentAgentService;

    @BeforeEach
    void setUp() {
        memoryToolService = mock(ExplicitMemoryToolService.class);
        currentAgentService = mock(CurrentAgentService.class);
        when(currentAgentService.getCurrentAgent())
                .thenReturn(new AuthenticatedAgent(UUID.randomUUID(), "agent-1"));
    }

    @Test
    void recallHandlerReturnsContextPayload() {
        RecallMemoryContextToolHandler handler = new RecallMemoryContextToolHandler(memoryToolService, currentAgentService);
        when(memoryToolService.recallContext(any())).thenReturn(new MemoryContext("assembled context", java.util.List.of()));

        ToolExecutionResult result = handler.execute(Map.of("conversation_id", "conv-1"));

        assertThat(result.isError()).isFalse();
        assertThat(result.getMemory().getContext()).isEqualTo("assembled context");
        verify(memoryToolService).recallContext(argThat((MemoryContextRecallRequest request) ->
                request != null && "agent-1".equals(request.agentId())
        ));
    }

    @Test
    void factHandlerReturnsStructuredFactPayload() {
        WriteMemoryFactToolHandler handler = new WriteMemoryFactToolHandler(memoryToolService, currentAgentService);
        MemoryEntry entry = new MemoryEntry();
        entry.setId(5L);
        entry.setAgentId("agent-1");
        entry.setScope(MemoryScope.AGENT);
        entry.setType(MemoryType.FACT);
        entry.setContent("A fact");
        entry.setImportance(0.8);
        entry.setStatus(MemoryStatus.ACTIVE);
        entry.setWriteMode(MemoryWriteMode.EXPLICIT);
        entry.setTags(Set.of("manual"));
        when(memoryToolService.writeFact(any())).thenReturn(entry);

        ToolExecutionResult result = handler.execute(Map.of("content", "A fact"));

        assertThat(result.isError()).isFalse();
        assertThat(result.getMemory().getFact().getContent()).isEqualTo("A fact");
        assertThat(result.getMemory().getFact().getWriteMode()).isEqualTo("EXPLICIT");
        verify(memoryToolService).writeFact(argThat((ExplicitFactMemoryWriteRequest request) ->
                request != null && "agent-1".equals(request.agentId())
        ));
    }

    @Test
    void ruleHandlerReturnsStructuredRulePayload() {
        WriteMemoryRuleToolHandler handler = new WriteMemoryRuleToolHandler(memoryToolService, currentAgentService);
        RuleEntry entry = new RuleEntry();
        entry.setId(7L);
        entry.setScope(RuleScope.AGENT);
        entry.setTitle("Rule");
        entry.setContent("Always test.");
        entry.setPriority(RulePriority.HIGH);
        entry.setStatus(RuleStatus.ACTIVE);
        entry.setWriteMode(MemoryWriteMode.EXPLICIT);
        when(memoryToolService.writeRule(any())).thenReturn(entry);

        ToolExecutionResult result = handler.execute(Map.of("title", "Rule", "content", "Always test."));

        assertThat(result.isError()).isFalse();
        assertThat(result.getMemory().getRule().getTitle()).isEqualTo("Rule");
        assertThat(result.getMemory().getRule().getWriteMode()).isEqualTo("EXPLICIT");
        verify(memoryToolService).writeRule(argThat((ExplicitRuleMemoryWriteRequest request) ->
                request != null && "agent-1".equals(request.agentId())
        ));
    }
}
