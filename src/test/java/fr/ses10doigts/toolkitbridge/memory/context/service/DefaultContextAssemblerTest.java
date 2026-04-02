package fr.ses10doigts.toolkitbridge.memory.context.service;

import fr.ses10doigts.toolkitbridge.memory.context.config.ContextAssemblerProperties;
import fr.ses10doigts.toolkitbridge.memory.context.model.ContextRequest;
import fr.ses10doigts.toolkitbridge.memory.conversation.port.ConversationMemoryService;
import fr.ses10doigts.toolkitbridge.memory.retrieval.model.MemoryQuery;
import fr.ses10doigts.toolkitbridge.memory.retrieval.port.MemoryRetriever;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleEntry;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RulePriority;
import fr.ses10doigts.toolkitbridge.memory.rule.service.RuleService;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryScope;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryType;
import org.junit.jupiter.api.Test;

import java.util.List;

import org.mockito.ArgumentCaptor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DefaultContextAssemblerTest {

    @Test
    void buildsContextInExpectedOrder() {
        RuleService ruleService = mock(RuleService.class);
        ConversationMemoryService conversationMemoryService = mock(ConversationMemoryService.class);
        MemoryRetriever memoryRetriever = mock(MemoryRetriever.class);
        ContextAssemblerProperties properties = new ContextAssemblerProperties();

        RuleEntry rule = rule("Always be safe", RulePriority.HIGH);
        MemoryEntry memory = memory("Remember X");

        when(ruleService.getApplicableRules("agent-1", "project-1"))
                .thenReturn(List.of(rule));
        when(memoryRetriever.retrieve(any()))
                .thenReturn(List.of(memory));
        when(conversationMemoryService.buildContext(any()))
                .thenReturn("Conversation content");

        DefaultContextAssembler assembler = new DefaultContextAssembler(
                ruleService,
                conversationMemoryService,
                memoryRetriever,
                properties
        );

        String context = assembler.buildContext(new ContextRequest(
                "agent-1",
                "conv-1",
                "project-1",
                "User says hello"
        ));

        int rulesIndex = context.indexOf("## Rules");
        int memoriesIndex = context.indexOf("## Relevant Memories");
        int conversationIndex = context.indexOf("## Conversation");
        int userIndex = context.indexOf("## User Input");

        assertThat(rulesIndex).isGreaterThanOrEqualTo(0);
        assertThat(memoriesIndex).isGreaterThan(rulesIndex);
        assertThat(conversationIndex).isGreaterThan(memoriesIndex);
        assertThat(userIndex).isGreaterThan(conversationIndex);

        assertThat(context).contains("- [HIGH] Always be safe");
        assertThat(context).contains("- Remember X");
        assertThat(context).contains("Conversation content");
        assertThat(context).contains("User says hello");
    }

    @Test
    void respectsRulesAndMemoriesLimits() {
        RuleService ruleService = mock(RuleService.class);
        ConversationMemoryService conversationMemoryService = mock(ConversationMemoryService.class);
        MemoryRetriever memoryRetriever = mock(MemoryRetriever.class);
        ContextAssemblerProperties properties = new ContextAssemblerProperties();
        properties.setMaxRules(1);
        properties.setMaxMemories(1);

        when(ruleService.getApplicableRules("agent-1", null))
                .thenReturn(List.of(rule("R1", RulePriority.LOW), rule("R2", RulePriority.HIGH)));
        when(memoryRetriever.retrieve(any()))
                .thenReturn(List.of(memory("M1"), memory("M2")));
        when(conversationMemoryService.buildContext(any()))
                .thenReturn("Conversation content");

        DefaultContextAssembler assembler = new DefaultContextAssembler(
                ruleService,
                conversationMemoryService,
                memoryRetriever,
                properties
        );

        String context = assembler.buildContext(new ContextRequest(
                "agent-1",
                "conv-1",
                null,
                "User says hello"
        ));

        assertThat(countOccurrences(context, "- [")).isEqualTo(1);
        assertThat(countOccurrences(context, "- M")).isEqualTo(1);
    }

    @Test
    void trimsContextWhenMaxCharactersExceeded() {
        RuleService ruleService = mock(RuleService.class);
        ConversationMemoryService conversationMemoryService = mock(ConversationMemoryService.class);
        MemoryRetriever memoryRetriever = mock(MemoryRetriever.class);

        when(ruleService.getApplicableRules("agent-1", null))
                .thenReturn(List.of(rule("Rule content", RulePriority.HIGH)));
        when(memoryRetriever.retrieve(any()))
                .thenReturn(List.of(memory("Memory content")));
        when(conversationMemoryService.buildContext(any()))
                .thenReturn("Conversation content");

        ContextAssemblerProperties large = new ContextAssemblerProperties();
        large.setMaxCharacters(1000);

        DefaultContextAssembler fullAssembler = new DefaultContextAssembler(
                ruleService,
                conversationMemoryService,
                memoryRetriever,
                large
        );

        String full = fullAssembler.buildContext(new ContextRequest(
                "agent-1",
                "conv-1",
                null,
                "User says hello"
        ));

        ContextAssemblerProperties small = new ContextAssemblerProperties();
        small.setMaxCharacters(50);

        DefaultContextAssembler trimmedAssembler = new DefaultContextAssembler(
                ruleService,
                conversationMemoryService,
                memoryRetriever,
                small
        );

        String trimmed = trimmedAssembler.buildContext(new ContextRequest(
                "agent-1",
                "conv-1",
                null,
                "User says hello"
        ));

        assertThat(trimmed.length()).isEqualTo(50);
        assertThat(trimmed).isEqualTo(full.substring(full.length() - 50));
    }

    @Test
    void rejectsNullRequest() {
        RuleService ruleService = mock(RuleService.class);
        ConversationMemoryService conversationMemoryService = mock(ConversationMemoryService.class);
        MemoryRetriever memoryRetriever = mock(MemoryRetriever.class);
        ContextAssemblerProperties properties = new ContextAssemblerProperties();

        DefaultContextAssembler assembler = new DefaultContextAssembler(
                ruleService,
                conversationMemoryService,
                memoryRetriever,
                properties
        );

        assertThatThrownBy(() -> assembler.buildContext(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("request");
    }

    @Test
    void skipsConversationWhenConversationIdMissing() {
        RuleService ruleService = mock(RuleService.class);
        ConversationMemoryService conversationMemoryService = mock(ConversationMemoryService.class);
        MemoryRetriever memoryRetriever = mock(MemoryRetriever.class);
        ContextAssemblerProperties properties = new ContextAssemblerProperties();

        when(ruleService.getApplicableRules("agent-1", null))
                .thenReturn(List.of(rule("R1", RulePriority.HIGH)));
        when(memoryRetriever.retrieve(any()))
                .thenReturn(List.of(memory("Memory")));

        DefaultContextAssembler assembler = new DefaultContextAssembler(
                ruleService,
                conversationMemoryService,
                memoryRetriever,
                properties
        );

        assembler.buildContext(new ContextRequest(
                "agent-1",
                null,
                null,
                "User says hello"
        ));

        verify(conversationMemoryService, never()).buildContext(any());
    }

    @Test
    void appliesMaxSemanticMemoriesOverride() {
        RuleService ruleService = mock(RuleService.class);
        ConversationMemoryService conversationMemoryService = mock(ConversationMemoryService.class);
        MemoryRetriever memoryRetriever = mock(MemoryRetriever.class);
        ContextAssemblerProperties properties = new ContextAssemblerProperties();

        when(ruleService.getApplicableRules("agent-1", null))
                .thenReturn(List.of(rule("R1", RulePriority.HIGH)));
        when(memoryRetriever.retrieve(any()))
                .thenReturn(List.of(memory("Memory")));
        when(conversationMemoryService.buildContext(any()))
                .thenReturn("Conversation content");

        DefaultContextAssembler assembler = new DefaultContextAssembler(
                ruleService,
                conversationMemoryService,
                memoryRetriever,
                properties
        );

        assembler.buildContext(new ContextRequest(
                "agent-1",
                "user-1",
                "bot-1",
                "project-1",
                "User says hello",
                "conv-1",
                3,
                null,
                null,
                null
        ));

        ArgumentCaptor<MemoryQuery> queryCaptor = ArgumentCaptor.forClass(MemoryQuery.class);
        verify(memoryRetriever).retrieve(queryCaptor.capture());
        MemoryQuery query = queryCaptor.getValue();
        assertThat(query.limit()).isEqualTo(3);
    }

    private RuleEntry rule(String content, RulePriority priority) {
        RuleEntry entry = new RuleEntry();
        entry.setContent(content);
        entry.setPriority(priority);
        return entry;
    }

    private MemoryEntry memory(String content) {
        MemoryEntry entry = new MemoryEntry();
        entry.setAgentId("agent-1");
        entry.setScope(MemoryScope.AGENT);
        entry.setType(MemoryType.FACT);
        entry.setContent(content);
        return entry;
    }

    private int countOccurrences(String input, String token) {
        int count = 0;
        int index = 0;
        while (true) {
            index = input.indexOf(token, index);
            if (index < 0) {
                return count;
            }
            count++;
            index += token.length();
        }
    }
}
