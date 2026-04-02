package fr.ses10doigts.toolkitbridge.memory.context.service;

import fr.ses10doigts.toolkitbridge.memory.context.config.ContextAssemblerProperties;
import fr.ses10doigts.toolkitbridge.memory.context.model.ContextRequest;
import fr.ses10doigts.toolkitbridge.memory.conversation.port.ConversationMemoryService;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeEventType;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeStatus;
import fr.ses10doigts.toolkitbridge.memory.retrieval.facade.MemoryRetrievalFacade;
import fr.ses10doigts.toolkitbridge.memory.retrieval.model.RetrievedMemories;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleEntry;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RulePriority;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryScope;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryType;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DefaultContextAssemblerTest {

    @Test
    void buildsContextInExpectedOrder() {
        MemoryRetrievalFacade retrievalFacade = mock(MemoryRetrievalFacade.class);
        ConversationMemoryService conversationMemoryService = mock(ConversationMemoryService.class);
        ContextAssemblerProperties properties = new ContextAssemblerProperties();

        RuleEntry rule = rule("Always be safe", RulePriority.HIGH);
        MemoryEntry memory = memory("Remember X");
        RetrievedMemories.EpisodeSummary episode = episode("agent-exchange", EpisodeStatus.SUCCESS);

        when(retrievalFacade.retrieve(any()))
                .thenReturn(retrieved(List.of(rule), List.of(memory), List.of(episode)));
        when(conversationMemoryService.buildContext(any()))
                .thenReturn("Conversation content");

        DefaultContextAssembler assembler = new DefaultContextAssembler(
                retrievalFacade,
                conversationMemoryService,
                properties
        );

        String context = assembler.buildContext(new ContextRequest(
                "agent-1",
                "conv-1",
                "project-1",
                "User says hello"
        ));

        int rulesIndex = context.indexOf("## Rules");
        int factsIndex = context.indexOf("## Known Facts");
        int episodesIndex = context.indexOf("## Recent Episodes");
        int conversationIndex = context.indexOf("## Conversation");
        int userIndex = context.indexOf("## User Input");

        assertThat(rulesIndex).isGreaterThanOrEqualTo(0);
        assertThat(factsIndex).isGreaterThan(rulesIndex);
        assertThat(episodesIndex).isGreaterThan(factsIndex);
        assertThat(conversationIndex).isGreaterThan(episodesIndex);
        assertThat(userIndex).isGreaterThan(conversationIndex);

        assertThat(context).contains("- [HIGH] Always be safe");
        assertThat(context).contains("- Remember X");
        assertThat(context).contains("- [SUCCESS] agent-exchange");
        assertThat(context).contains("Conversation content");
        assertThat(context).contains("User says hello");
    }

    @Test
    void respectsRulesAndMemoriesLimits() {
        MemoryRetrievalFacade retrievalFacade = mock(MemoryRetrievalFacade.class);
        ConversationMemoryService conversationMemoryService = mock(ConversationMemoryService.class);
        ContextAssemblerProperties properties = new ContextAssemblerProperties();
        properties.setMaxRules(1);
        properties.setMaxMemories(1);

        List<RuleEntry> rules = List.of(rule("R1", RulePriority.LOW), rule("R2", RulePriority.HIGH));
        List<MemoryEntry> memories = List.of(memory("M1"), memory("M2"));
        when(retrievalFacade.retrieve(any()))
                .thenReturn(retrieved(rules, memories, List.of()));
        when(conversationMemoryService.buildContext(any()))
                .thenReturn("");

        DefaultContextAssembler assembler = new DefaultContextAssembler(
                retrievalFacade,
                conversationMemoryService,
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
        MemoryRetrievalFacade retrievalFacade = mock(MemoryRetrievalFacade.class);
        ConversationMemoryService conversationMemoryService = mock(ConversationMemoryService.class);

        RetrievedMemories data = retrieved(
                List.of(rule("Rule content", RulePriority.HIGH)),
                List.of(memory("Memory content")),
                List.of()
        );
        when(retrievalFacade.retrieve(any())).thenReturn(data);
        when(conversationMemoryService.buildContext(any())).thenReturn("Conversation content");

        ContextAssemblerProperties large = new ContextAssemblerProperties();
        large.setMaxCharacters(1000);

        DefaultContextAssembler fullAssembler = new DefaultContextAssembler(
                retrievalFacade,
                conversationMemoryService,
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
                retrievalFacade,
                conversationMemoryService,
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
        MemoryRetrievalFacade retrievalFacade = mock(MemoryRetrievalFacade.class);
        ConversationMemoryService conversationMemoryService = mock(ConversationMemoryService.class);
        ContextAssemblerProperties properties = new ContextAssemblerProperties();

        when(retrievalFacade.retrieve(any()))
                .thenReturn(retrieved(List.of(rule("R1", RulePriority.HIGH)), List.of(memory("M1")), List.of()));

        DefaultContextAssembler assembler = new DefaultContextAssembler(
                retrievalFacade,
                conversationMemoryService,
                properties
        );

        assertThatThrownBy(() -> assembler.buildContext(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("request");
    }

    private RetrievedMemories retrieved(List<RuleEntry> rules,
                                        List<MemoryEntry> memories,
                                        List<RetrievedMemories.EpisodeSummary> episodes) {
        return new RetrievedMemories(rules, memories, episodes, "conversation");
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

    private RetrievedMemories.EpisodeSummary episode(String summary, EpisodeStatus status) {
        return new RetrievedMemories.EpisodeSummary(
                EpisodeEventType.RESULT,
                status,
                summary,
                Instant.parse("2025-01-01T00:00:00Z"),
                "AGENT",
                "conv-1"
        );
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
