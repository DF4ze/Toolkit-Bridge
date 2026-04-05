package fr.ses10doigts.toolkitbridge.memory.context.service;

import fr.ses10doigts.toolkitbridge.memory.context.config.ContextAssemblerProperties;
import fr.ses10doigts.toolkitbridge.memory.context.global.model.SharedGlobalContextSnapshot;
import fr.ses10doigts.toolkitbridge.memory.context.global.port.SharedGlobalContextProvider;
import fr.ses10doigts.toolkitbridge.memory.context.model.AssembledContext;
import fr.ses10doigts.toolkitbridge.memory.context.model.ContextRequest;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeEventType;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeStatus;
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

class DefaultContextAssemblerTest {

    private static final SharedGlobalContextProvider EMPTY_GLOBAL_CONTEXT =
            () -> new SharedGlobalContextSnapshot("", List.of(), Instant.parse("2025-01-01T00:00:00Z"));

    @Test
    void buildsContextInExpectedOrder() {
        ContextAssemblerProperties properties = new ContextAssemblerProperties();

        RuleEntry rule = rule("Always be safe", RulePriority.HIGH);
        MemoryEntry memory = memory("Remember X");
        RetrievedMemories.EpisodeSummary episode = episode("agent-exchange", EpisodeStatus.SUCCESS);

        DefaultContextAssembler assembler = new DefaultContextAssembler(
                properties,
                EMPTY_GLOBAL_CONTEXT
        );

        AssembledContext assembled = assembler.buildContext(
                new ContextRequest("agent-1", "conv-1", "project-1", "User says hello"),
                retrieved(List.of(rule), List.of(memory), List.of(episode), "Conversation content")
        );

        String context = assembled.text();

        int rulesIndex = context.indexOf("## Rules");
        int factsIndex = context.indexOf("## Facts");
        int episodesIndex = context.indexOf("## Recent Episodes");
        int conversationIndex = context.indexOf("## Conversation");
        int userIndex = context.indexOf("## User Input");

        assertThat(rulesIndex).isGreaterThanOrEqualTo(0);
        assertThat(factsIndex).isGreaterThan(rulesIndex);
        assertThat(episodesIndex).isGreaterThan(factsIndex);
        assertThat(conversationIndex).isGreaterThan(episodesIndex);
        assertThat(userIndex).isGreaterThan(conversationIndex);

        assertThat(context).contains("### Agent Context");
        assertThat(context).contains("- [HIGH] Always be safe");
        assertThat(context).contains("- Remember X");
        assertThat(context).contains("- [SUCCESS] agent-exchange");
        assertThat(context).contains("Conversation content");
        assertThat(context).contains("User says hello");
        assertThat(assembled.injectedSemanticMemoryIds()).isEmpty();
    }

    @Test
    void respectsRulesAndMemoriesLimits() {
        ContextAssemblerProperties properties = new ContextAssemblerProperties();
        properties.setMaxRules(1);
        properties.setMaxMemories(1);

        List<RuleEntry> rules = List.of(rule("R1", RulePriority.LOW), rule("R2", RulePriority.HIGH));
        List<MemoryEntry> memories = List.of(memory("M1"), memory("M2"));

        DefaultContextAssembler assembler = new DefaultContextAssembler(
                properties,
                EMPTY_GLOBAL_CONTEXT
        );

        String context = assembler.buildContext(
                new ContextRequest("agent-1", "conv-1", null, "User says hello"),
                retrieved(rules, memories, List.of(), "")
        ).text();

        assertThat(countOccurrences(context, "- [")).isEqualTo(1);
        assertThat(countOccurrences(context, "- M")).isEqualTo(1);
    }

    @Test
    void trimsContextWhenMaxCharactersExceeded() {
        RetrievedMemories data = retrieved(
                List.of(rule("Rule content", RulePriority.HIGH)),
                List.of(memory("Memory content")),
                List.of(),
                "Conversation content"
        );

        ContextAssemblerProperties large = new ContextAssemblerProperties();
        large.setMaxCharacters(1000);

        DefaultContextAssembler fullAssembler = new DefaultContextAssembler(
                large,
                EMPTY_GLOBAL_CONTEXT
        );

        String full = fullAssembler.buildContext(
                new ContextRequest("agent-1", "conv-1", null, "User says hello"),
                data
        ).text();

        ContextAssemblerProperties small = new ContextAssemblerProperties();
        small.setMaxCharacters(50);

        DefaultContextAssembler trimmedAssembler = new DefaultContextAssembler(
                small,
                EMPTY_GLOBAL_CONTEXT
        );

        String trimmed = trimmedAssembler.buildContext(
                new ContextRequest("agent-1", "conv-1", null, "User says hello"),
                data
        ).text();

        assertThat(trimmed.length()).isEqualTo(50);
        assertThat(trimmed).contains("## User Input");
        assertThat(trimmed).contains("User says hello");
        assertThat(trimmed).startsWith("## Rules");
    }

    @Test
    void rejectsNullRequest() {
        ContextAssemblerProperties properties = new ContextAssemblerProperties();

        DefaultContextAssembler assembler = new DefaultContextAssembler(
                properties,
                EMPTY_GLOBAL_CONTEXT
        );

        assertThatThrownBy(() -> assembler.buildContext(null, retrieved(
                List.of(rule("R1", RulePriority.HIGH)),
                List.of(memory("M1")),
                List.of(),
                ""
        )))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("request");
    }

    @Test
    void removesDuplicateFactsFromSameBatch() {
        ContextAssemblerProperties properties = new ContextAssemblerProperties();

        MemoryEntry sameA = memory("Same");
        sameA.setId(10L);
        MemoryEntry sameB = memory("Same");
        sameB.setId(11L);
        MemoryEntry other = memory("Other");
        other.setId(12L);

        DefaultContextAssembler assembler = new DefaultContextAssembler(properties, EMPTY_GLOBAL_CONTEXT);

        AssembledContext assembled = assembler.buildContext(
                new ContextRequest("agent-1", "conv-1", null, "hello"),
                retrieved(List.of(), List.of(sameA, sameB, other), List.of(), "")
        );

        String context = assembled.text();

        assertThat(countOccurrences(context, "- Same")).isEqualTo(1);
        assertThat(countOccurrences(context, "- Other")).isEqualTo(1);
        assertThat(assembled.injectedSemanticMemoryIds()).containsExactly(10L, 12L);
    }

    @Test
    void appliesRequestOverridesForMemoriesAndEpisodes() {
        ContextAssemblerProperties properties = new ContextAssemblerProperties();
        properties.setMaxMemories(5);
        properties.setMaxEpisodes(5);

        MemoryEntry m1 = memory("M1");
        m1.setId(1L);
        MemoryEntry m2 = memory("M2");
        m2.setId(2L);

        RetrievedMemories.EpisodeSummary e1 = episode("E1", EpisodeStatus.SUCCESS);
        RetrievedMemories.EpisodeSummary e2 = episode("E2", EpisodeStatus.SUCCESS);

        DefaultContextAssembler assembler = new DefaultContextAssembler(properties, EMPTY_GLOBAL_CONTEXT);
        AssembledContext assembled = assembler.buildContext(
                new ContextRequest("agent-1", "conv-1", null, "hello", 1, 1),
                retrieved(List.of(), List.of(m1, m2), List.of(e1, e2), "")
        );

        String context = assembled.text();
        assertThat(countOccurrences(context, "- M")).isEqualTo(1);
        assertThat(countOccurrences(context, "- [SUCCESS] E")).isEqualTo(1);
        assertThat(assembled.injectedSemanticMemoryIds()).containsExactly(1L);
    }

    @Test
    void keepsUserInputEvenWhenPromptNeedsTrimming() {
        ContextAssemblerProperties properties = new ContextAssemblerProperties();
        properties.setMaxCharacters(90);
        properties.setMaxRules(10);
        properties.setMaxMemories(10);
        properties.setMaxEpisodes(10);

        RuleEntry rule = rule("This is a very long mandatory rule that should be present first", RulePriority.HIGH);
        MemoryEntry memory = memory("This is a long fact that may be truncated if required");

        DefaultContextAssembler assembler = new DefaultContextAssembler(properties, EMPTY_GLOBAL_CONTEXT);
        String context = assembler.buildContext(
                new ContextRequest("agent-1", "conv-1", null, "Critical user input"),
                retrieved(List.of(rule), List.of(memory), List.of(), "Long conversation that should be dropped first")
        ).text();

        assertThat(context.length()).isLessThanOrEqualTo(90);
        assertThat(context).contains("## User Input");
        assertThat(context).contains("Critical user input");
    }

    @Test
    void injectsSharedGlobalContextBeforeFacts() {
        ContextAssemblerProperties properties = new ContextAssemblerProperties();
        SharedGlobalContextProvider globalContextProvider =
                () -> new SharedGlobalContextSnapshot("### profile.md\nName: Alice", List.of("profile.md"), Instant.parse("2025-01-01T00:00:00Z"));

        DefaultContextAssembler assembler = new DefaultContextAssembler(properties, globalContextProvider);

        String context = assembler.buildContext(
                new ContextRequest("agent-1", "conv-1", null, "hello"),
                retrieved(List.of(rule("Follow rules", RulePriority.HIGH)), List.of(memory("Fact")), List.of(), "")
        ).text();

        assertThat(context).contains("## Shared Global Context");
        assertThat(context.indexOf("## Shared Global Context")).isGreaterThan(context.indexOf("## Rules"));
        assertThat(context.indexOf("## Shared Global Context")).isLessThan(context.indexOf("## Facts"));
        assertThat(context).contains("Name: Alice");
    }

    private RetrievedMemories retrieved(List<RuleEntry> rules,
                                        List<MemoryEntry> memories,
                                        List<RetrievedMemories.EpisodeSummary> episodes,
                                        String conversationSlice) {
        return new RetrievedMemories(rules, memories, episodes, conversationSlice);
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
