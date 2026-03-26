package fr.ses10doigts.toolkitbridge.memory.conversation.service;

import fr.ses10doigts.toolkitbridge.memory.conversation.config.ConversationMemoryProperties;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationCompressionReason;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMessage;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationRole;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationSummary;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SimpleConversationSummarizerTest {

    @Test
    void summarizesMessagesAndTruncatesWhenNeeded() {
        ConversationMemoryProperties props = new ConversationMemoryProperties();
        props.setMaxSummaryCharacters(20);

        SimpleConversationSummarizer summarizer = new SimpleConversationSummarizer(props);
        ConversationMessage message = new ConversationMessage(
                UUID.randomUUID(),
                "agent-1",
                "conv-1",
                ConversationRole.USER,
                "1234567890123456789012345",
                Instant.now(),
                Map.of()
        );

        ConversationSummary summary = summarizer.summarize(
                "agent-1",
                "conv-1",
                List.of(message),
                ConversationCompressionReason.MAX_MESSAGES_EXCEEDED
        );

        assertThat(summary.content()).contains("[summary truncated]");
    }

    @Test
    void sanitizesNullContent() {
        ConversationMemoryProperties props = new ConversationMemoryProperties();
        SimpleConversationSummarizer summarizer = new SimpleConversationSummarizer(props);

        ConversationMessage message = new ConversationMessage(
                UUID.randomUUID(),
                "agent-1",
                "conv-1",
                ConversationRole.USER,
                null,
                Instant.now(),
                Map.of()
        );

        ConversationSummary summary = summarizer.summarize(
                "agent-1",
                "conv-1",
                List.of(message),
                ConversationCompressionReason.MAX_MESSAGES_EXCEEDED
        );

        assertThat(summary.content()).contains("[USER]");
    }
}

