package fr.ses10doigts.toolkitbridge.memory.conversation.service;

import fr.ses10doigts.toolkitbridge.memory.conversation.config.ConversationMemoryProperties;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationCompressionReason;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMemoryKey;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMemoryState;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMessage;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationRole;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationSummary;
import fr.ses10doigts.toolkitbridge.memory.conversation.port.ConversationContextRenderer;
import fr.ses10doigts.toolkitbridge.memory.conversation.port.ConversationMemoryService;
import fr.ses10doigts.toolkitbridge.memory.conversation.port.ConversationMemoryStore;
import fr.ses10doigts.toolkitbridge.memory.conversation.port.ConversationSummarizer;
import fr.ses10doigts.toolkitbridge.memory.conversation.store.InMemoryConversationMemoryStore;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DefaultConversationMemoryServiceTest {

    @Test
    void appendMessageCreatesInitialState() {
        ConversationMemoryProperties props = baseProps();
        ConversationMemoryStore store = new InMemoryConversationMemoryStore(Duration.ofMinutes(10));
        ConversationSummarizer summarizer = new SimpleConversationSummarizer(props);
        ConversationContextRenderer renderer = new DefaultConversationContextRenderer();
        ConversationMemoryService service = new DefaultConversationMemoryService(store, summarizer, renderer, props);

        ConversationMemoryKey key = new ConversationMemoryKey("agent-1", "conv-1");
        ConversationMessage message = message("agent-1", "conv-1", "hello");

        ConversationMemoryState state = service.appendMessage(key, message);

        assertThat(state.recentMessages()).hasSize(1);
        assertThat(state.summaries()).isEmpty();
    }

    @Test
    void compactWhenMaxMessagesExceeded() {
        ConversationMemoryProperties props = baseProps();
        props.setMaxRecentMessages(3);
        props.setMinMessagesToKeep(2);
        props.setAutoSummarize(true);

        ConversationMemoryStore store = new InMemoryConversationMemoryStore(Duration.ofMinutes(10));
        ConversationSummarizer summarizer = summaryCapturingSummarizer();
        ConversationContextRenderer renderer = new DefaultConversationContextRenderer();
        ConversationMemoryService service = new DefaultConversationMemoryService(store, summarizer, renderer, props);

        ConversationMemoryKey key = new ConversationMemoryKey("agent-1", "conv-1");
        for (int i = 0; i < 4; i++) {
            service.appendMessage(key, message("agent-1", "conv-1", "msg-" + i));
        }

        ConversationMemoryState state = service.getState(key);
        assertThat(state.summaries()).hasSize(1);
        assertThat(state.recentMessages()).hasSize(2);
        assertThat(state.summaries().get(0).reason()).isEqualTo(ConversationCompressionReason.MAX_MESSAGES_EXCEEDED);
        assertThat(state.summaries().get(0).summarizedMessageCount()).isEqualTo(2);
    }

    @Test
    void compactWhenMaxCharactersExceeded() {
        ConversationMemoryProperties props = baseProps();
        props.setMaxRecentCharacters(10);
        props.setMinMessagesToKeep(2);
        props.setMaxRecentMessages(5);
        props.setAutoSummarize(true);

        ConversationMemoryStore store = new InMemoryConversationMemoryStore(Duration.ofMinutes(10));
        ConversationSummarizer summarizer = summaryCapturingSummarizer();
        ConversationContextRenderer renderer = new DefaultConversationContextRenderer();
        ConversationMemoryService service = new DefaultConversationMemoryService(store, summarizer, renderer, props);

        ConversationMemoryKey key = new ConversationMemoryKey("agent-1", "conv-1");
        service.appendMessage(key, message("agent-1", "conv-1", "12345"));
        service.appendMessage(key, message("agent-1", "conv-1", "67890"));
        service.appendMessage(key, message("agent-1", "conv-1", "abc"));

        ConversationMemoryState state = service.getState(key);
        assertThat(state.summaries()).hasSize(1);
        assertThat(state.summaries().get(0).reason()).isEqualTo(ConversationCompressionReason.MAX_CHARACTERS_EXCEEDED);
        assertThat(state.recentMessages()).hasSize(2);
    }

    @Test
    void keepsMinimumMessages() {
        ConversationMemoryProperties props = baseProps();
        props.setMaxRecentMessages(4);
        props.setMinMessagesToKeep(3);
        props.setAutoSummarize(true);

        ConversationMemoryStore store = new InMemoryConversationMemoryStore(Duration.ofMinutes(10));
        ConversationSummarizer summarizer = summaryCapturingSummarizer();
        ConversationContextRenderer renderer = new DefaultConversationContextRenderer();
        ConversationMemoryService service = new DefaultConversationMemoryService(store, summarizer, renderer, props);

        ConversationMemoryKey key = new ConversationMemoryKey("agent-1", "conv-1");
        for (int i = 0; i < 5; i++) {
            service.appendMessage(key, message("agent-1", "conv-1", "msg-" + i));
        }

        ConversationMemoryState state = service.getState(key);
        assertThat(state.recentMessages()).hasSize(3);
        assertThat(state.summaries()).hasSize(1);
    }

    @Test
    void buildContextRendersSummaryThenRecentMessages() {
        ConversationMemoryProperties props = baseProps();
        props.setMaxRecentMessages(2);
        props.setMinMessagesToKeep(1);
        props.setAutoSummarize(true);

        ConversationMemoryStore store = new InMemoryConversationMemoryStore(Duration.ofMinutes(10));
        ConversationSummarizer summarizer = summaryCapturingSummarizer();
        ConversationContextRenderer renderer = new DefaultConversationContextRenderer();
        ConversationMemoryService service = new DefaultConversationMemoryService(store, summarizer, renderer, props);

        ConversationMemoryKey key = new ConversationMemoryKey("agent-1", "conv-1");
        service.appendMessage(key, message("agent-1", "conv-1", "one"));
        service.appendMessage(key, message("agent-1", "conv-1", "two"));
        service.appendMessage(key, message("agent-1", "conv-1", "three"));

        String context = service.buildContext(key);
        int summaryIndex = context.indexOf("## Conversation summary");
        int recentIndex = context.indexOf("## Recent conversation");

        assertThat(summaryIndex).isGreaterThanOrEqualTo(0);
        assertThat(recentIndex).isGreaterThan(summaryIndex);
    }

    @Test
    void getStateReturnsEmptyStateForUnknownConversation() {
        ConversationMemoryProperties props = baseProps();
        ConversationMemoryStore store = new InMemoryConversationMemoryStore(Duration.ofMinutes(10));
        ConversationSummarizer summarizer = summaryCapturingSummarizer();
        ConversationContextRenderer renderer = new DefaultConversationContextRenderer();
        ConversationMemoryService service = new DefaultConversationMemoryService(store, summarizer, renderer, props);

        ConversationMemoryState state = service.getState(new ConversationMemoryKey("agent-1", "conv-x"));

        assertThat(state.recentMessages()).isEmpty();
        assertThat(state.summaries()).isEmpty();
    }

    private ConversationMessage message(String agentId, String conversationId, String content) {
        return new ConversationMessage(
                UUID.randomUUID(),
                agentId,
                conversationId,
                ConversationRole.USER,
                content,
                Instant.now(),
                Map.of()
        );
    }

    private ConversationMemoryProperties baseProps() {
        ConversationMemoryProperties props = new ConversationMemoryProperties();
        props.setMaxRecentMessages(3);
        props.setMaxRecentCharacters(1000);
        props.setMinMessagesToKeep(2);
        props.setAutoSummarize(true);
        props.setMaxSummaryCharacters(200);
        return props;
    }

    private ConversationSummarizer summaryCapturingSummarizer() {
        return (agentId, conversationId, messages, reason) -> new ConversationSummary(
                UUID.randomUUID(),
                agentId,
                conversationId,
                "summary:" + messages.size(),
                messages.size(),
                messages.isEmpty() ? Instant.now() : messages.get(0).createdAt(),
                messages.isEmpty() ? Instant.now() : messages.get(messages.size() - 1).createdAt(),
                Instant.now(),
                reason
        );
    }
}

