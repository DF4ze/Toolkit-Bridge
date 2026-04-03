package fr.ses10doigts.toolkitbridge.memory.context.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContextRequestTest {

    @Test
    void normalizesValues() {
        ContextRequest request = new ContextRequest(
                " agent-1 ",
                " conv-1 ",
                " ",
                " hello ",
                3,
                2
        );

        assertThat(request.agentId()).isEqualTo("agent-1");
        assertThat(request.conversationId()).isEqualTo("conv-1");
        assertThat(request.projectId()).isNull();
        assertThat(request.currentUserMessage()).isEqualTo("hello");
        assertThat(request.maxSemanticMemories()).isEqualTo(3);
        assertThat(request.maxEpisodes()).isEqualTo(2);
    }

    @Test
    void rejectsInvalidLimits() {
        assertThatThrownBy(() -> new ContextRequest("agent-1", "conv-1", null, "hello", 0, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxSemanticMemories");

        assertThatThrownBy(() -> new ContextRequest("agent-1", "conv-1", null, "hello", 1, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maxEpisodes");
    }
}
