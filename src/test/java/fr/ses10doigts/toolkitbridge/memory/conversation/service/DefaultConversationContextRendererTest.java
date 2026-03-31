package fr.ses10doigts.toolkitbridge.memory.conversation.service;

import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMemoryState;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMessage;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationRole;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationSummary;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultConversationContextRendererTest {

    @Test
    void rendersSummaryBeforeRecentMessages() {
        DefaultConversationContextRenderer renderer = new DefaultConversationContextRenderer();

        ConversationSummary summary = new ConversationSummary(
                UUID.randomUUID(),
                "agent-1",
                "conv-1",
                "summary-content",
                2,
                Instant.now(),
                Instant.now(),
                Instant.now(),
                null
        );

        ConversationMessage message = new ConversationMessage(
                UUID.randomUUID(),
                "agent-1",
                "conv-1",
                ConversationRole.USER,
                "hello",
                Instant.now(),
                Map.of()
        );

        ConversationMemoryState state = new ConversationMemoryState(
                "agent-1",
                "conv-1",
                List.of(summary),
                List.of(message),
                Instant.now()
        );

        String rendered = renderer.render(state);
        assertThat(rendered).contains("## Conversation summary");
        assertThat(rendered).contains("## Recent conversation");
        assertThat(rendered.indexOf("summary-content")).isLessThan(rendered.indexOf("Recent conversation"));
    }
}

