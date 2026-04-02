package fr.ses10doigts.toolkitbridge.memory.context.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContextRequestTest {

    @Test
    void rejectsBlankAgentId() {
        assertThatThrownBy(() -> new ContextRequest(
                        "",
                        "conv-1",
                        "project-1",
                        "message"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("agentId");
    }

    @Test
    void rejectsBlankCurrentUserMessage() {
        assertThatThrownBy(() -> new ContextRequest(
                        "agent-1",
                        "conv-1",
                        "project-1",
                        "  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("currentUserMessage");
    }

    @Test
    void normalizesEmptyOptionals() {
        ContextRequest request = new ContextRequest(
                "agent-1",
                " user-1 ",
                " ",
                " project-1 ",
                "Hello",
                "conv-1",
                5,
                10,
                15,
                100
        );

        assertThat(request.userId()).isEqualTo("user-1");
        assertThat(request.botId()).isNull();
        assertThat(request.projectId()).isEqualTo("project-1");
        assertThat(request.conversationId()).isEqualTo("conv-1");
    }

    @Test
    void rejectsNonPositiveLimits() {
        assertThatThrownBy(() -> new ContextRequest(
                        "agent-1",
                        "user-1",
                        "bot-1",
                        "project-1",
                        "Hello",
                        "conv-1",
                        0,
                        null,
                        null,
                        null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxSemanticMemories");

        assertThatThrownBy(() -> new ContextRequest(
                        "agent-1",
                        "user-1",
                        "bot-1",
                        "project-1",
                        "Hello",
                        "conv-1",
                        5,
                        -1,
                        null,
                        null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxEpisodes");

        assertThatThrownBy(() -> new ContextRequest(
                        "agent-1",
                        "user-1",
                        "bot-1",
                        "project-1",
                        "Hello",
                        "conv-1",
                        5,
                        5,
                        0,
                        null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxConversationMessages");

        assertThatThrownBy(() -> new ContextRequest(
                        "agent-1",
                        "user-1",
                        "bot-1",
                        "project-1",
                        "Hello",
                        "conv-1",
                        5,
                        5,
                        5,
                        0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tokenBudgetHint");
    }
}
