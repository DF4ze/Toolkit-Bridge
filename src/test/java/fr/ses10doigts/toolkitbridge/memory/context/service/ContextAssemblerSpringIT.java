package fr.ses10doigts.toolkitbridge.memory.context.service;

import fr.ses10doigts.toolkitbridge.memory.context.model.AssembledContext;
import fr.ses10doigts.toolkitbridge.memory.context.model.ContextRequest;
import fr.ses10doigts.toolkitbridge.memory.context.port.ContextAssembler;
import fr.ses10doigts.toolkitbridge.memory.retrieval.model.RetrievedMemories;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleEntry;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RulePriority;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryScope;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "toolkit.memory.context.max-rules=2",
        "toolkit.memory.context.max-memories=2",
        "toolkit.memory.context.max-characters=500"
})
class ContextAssemblerSpringIT {

    @Autowired
    private ContextAssembler assembler;

    @Test
    void assemblesContextWithStructuredRetrievedMemories() {
        RuleEntry rule = new RuleEntry();
        rule.setContent("Always be safe");
        rule.setPriority(RulePriority.HIGH);

        MemoryEntry memory = new MemoryEntry();
        memory.setId(42L);
        memory.setAgentId("agent-1");
        memory.setScope(MemoryScope.AGENT);
        memory.setType(MemoryType.FACT);
        memory.setContent("Memory content");

        RetrievedMemories retrieved = new RetrievedMemories(
                List.of(rule),
                List.of(memory),
                List.of(),
                "Conversation content"
        );

        AssembledContext context = assembler.buildContext(
                new ContextRequest("agent-1", "conv-1", "project-1", "User says hello"),
                retrieved
        );

        assertThat(context.text()).contains("## Rules");
        assertThat(context.text()).contains("## Facts");
        assertThat(context.text()).contains("### Agent Context");
        assertThat(context.text()).contains("## Conversation");
        assertThat(context.text()).contains("## User Input");
        assertThat(context.text()).contains("User says hello");
        assertThat(context.injectedSemanticMemoryIds()).containsExactly(42L);
    }
}
