package fr.ses10doigts.toolkitbridge.memory.tool.service;

import fr.ses10doigts.toolkitbridge.memory.facade.MemoryFacade;
import fr.ses10doigts.toolkitbridge.memory.facade.model.MemoryContext;
import fr.ses10doigts.toolkitbridge.memory.facade.model.MemoryContextRequest;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleEntry;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleScope;
import fr.ses10doigts.toolkitbridge.memory.rule.service.RuleService;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryScope;
import fr.ses10doigts.toolkitbridge.memory.semantic.scope.MemoryScopePolicy;
import fr.ses10doigts.toolkitbridge.memory.semantic.service.SemanticMemoryService;
import fr.ses10doigts.toolkitbridge.memory.shared.model.MemoryWriteMode;
import fr.ses10doigts.toolkitbridge.memory.tool.model.ExplicitFactMemoryWriteRequest;
import fr.ses10doigts.toolkitbridge.memory.tool.model.ExplicitRuleMemoryWriteRequest;
import fr.ses10doigts.toolkitbridge.memory.tool.model.MemoryContextRecallRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ExplicitMemoryToolServiceTest {

    private MemoryFacade memoryFacade;
    private SemanticMemoryService semanticMemoryService;
    private RuleService ruleService;
    private ExplicitMemoryToolService service;

    @BeforeEach
    void setUp() {
        memoryFacade = mock(MemoryFacade.class);
        semanticMemoryService = mock(SemanticMemoryService.class);
        ruleService = mock(RuleService.class);

        service = new ExplicitMemoryToolService(
                memoryFacade,
                semanticMemoryService,
                ruleService,
                new MemoryScopePolicy()
        );
    }

    @Test
    void recallContextUsesExistingMemoryFacadePipeline() {
        when(memoryFacade.buildContext(any(MemoryContextRequest.class)))
                .thenReturn(new MemoryContext("## Facts\n- durable fact\n\n## User Input\nFocus on recent facts", List.of(1L, 2L)));

        MemoryContext result = service.recallContext(new MemoryContextRecallRequest(
                "agent-42",
                "user-1",
                "project-1",
                "conversation-1",
                "Focus on recent facts",
                5,
                3
        ));

        assertThat(result.text()).isEqualTo("## Facts\n- durable fact");

        ArgumentCaptor<MemoryContextRequest> captor = ArgumentCaptor.forClass(MemoryContextRequest.class);
        verify(memoryFacade).buildContext(captor.capture());
        assertThat(captor.getValue().agentId()).isEqualTo("agent-42");
        assertThat(captor.getValue().conversationId()).isEqualTo("conversation-1");
        assertThat(captor.getValue().projectId()).isEqualTo("project-1");
    }

    @Test
    void writeFactMarksEntryAsExplicitAndUsesResolvedScope() {
        when(semanticMemoryService.create(any(MemoryEntry.class))).thenAnswer(invocation -> {
            MemoryEntry entry = invocation.getArgument(0);
            entry.setId(10L);
            return entry;
        });

        MemoryEntry result = service.writeFact(new ExplicitFactMemoryWriteRequest(
                null,
                "agent-42",
                "user-7",
                "project-9",
                null,
                null,
                null,
                "The project uses Spring Boot.",
                null,
                Set.of("manual")
        ));

        assertThat(result.getWriteMode()).isEqualTo(MemoryWriteMode.EXPLICIT);
        assertThat(result.getScope()).isEqualTo(MemoryScope.PROJECT);
        assertThat(result.getScopeId()).isEqualTo("project-9");
        assertThat(result.getAgentId()).isEqualTo("agent-42");
    }

    @Test
    void writeRuleDefaultsToProjectScopeAndMarksExplicit() {
        when(ruleService.create(any(RuleEntry.class))).thenAnswer(invocation -> {
            RuleEntry entry = invocation.getArgument(0);
            entry.setId(22L);
            return entry;
        });

        RuleEntry result = service.writeRule(new ExplicitRuleMemoryWriteRequest(
                null,
                "agent-42",
                "project-2",
                null,
                null,
                "Use constructor injection",
                "Always use constructor injection in services.",
                null
        ));

        assertThat(result.getWriteMode()).isEqualTo(MemoryWriteMode.EXPLICIT);
        assertThat(result.getScope()).isEqualTo(RuleScope.PROJECT);
        assertThat(result.getScopeId()).isEqualTo("project-2");
        assertThat(result.getAgentId()).isNull();
    }
}
