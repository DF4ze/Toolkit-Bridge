package fr.ses10doigts.toolkitbridge.memory.facade.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MemoryContextRequestTest {

    @Test
    void normalizesOptionalFields() {
        MemoryContextRequest request = new MemoryContextRequest(
                " agent-1 ",
                " user-1 ",
                " ",
                " project-1 ",
                " hello ",
                " conv-1 ",
                5,
                3,
                8,
                400
        );

        assertThat(request.agentId()).isEqualTo("agent-1");
        assertThat(request.userId()).isEqualTo("user-1");
        assertThat(request.botId()).isNull();
        assertThat(request.projectId()).isEqualTo("project-1");
        assertThat(request.currentUserMessage()).isEqualTo("hello");
        assertThat(request.conversationId()).isEqualTo("conv-1");
    }

    @Test
    void rejectsNonPositiveLimits() {
        assertThatThrownBy(() -> new MemoryContextRequest("agent-1", null, null, null, "hello", "conv-1", 0, 1, 1, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxSemanticMemories");

        assertThatThrownBy(() -> new MemoryContextRequest("agent-1", null, null, null, "hello", "conv-1", 1, 0, 1, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxEpisodes");

        assertThatThrownBy(() -> new MemoryContextRequest("agent-1", null, null, null, "hello", "conv-1", 1, 1, 0, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxConversationMessages");

        assertThatThrownBy(() -> new MemoryContextRequest("agent-1", null, null, null, "hello", "conv-1", 1, 1, 1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tokenBudgetHint");
    }
}
