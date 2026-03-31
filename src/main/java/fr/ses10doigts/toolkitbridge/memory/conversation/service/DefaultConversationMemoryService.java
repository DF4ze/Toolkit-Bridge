package fr.ses10doigts.toolkitbridge.memory.conversation.service;

import fr.ses10doigts.toolkitbridge.memory.conversation.config.ConversationMemoryProperties;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationCompressionReason;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMemoryKey;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMemoryState;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMessage;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationSummary;
import fr.ses10doigts.toolkitbridge.memory.conversation.port.ConversationContextRenderer;
import fr.ses10doigts.toolkitbridge.memory.conversation.port.ConversationMemoryService;
import fr.ses10doigts.toolkitbridge.memory.conversation.port.ConversationMemoryStore;
import fr.ses10doigts.toolkitbridge.memory.conversation.port.ConversationSummarizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
@ConditionalOnProperty(prefix = "toolkit.memory.conversation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DefaultConversationMemoryService implements ConversationMemoryService {

    private final ConversationMemoryStore store;
    private final ConversationSummarizer summarizer;
    private final ConversationContextRenderer renderer;
    private final ConversationMemoryProperties properties;

    public DefaultConversationMemoryService(
            ConversationMemoryStore store,
            ConversationSummarizer summarizer,
            ConversationContextRenderer renderer,
            ConversationMemoryProperties properties
    ) {
        this.store = store;
        this.summarizer = summarizer;
        this.renderer = renderer;
        this.properties = properties;
        validateProperties(properties);
    }

    @Override
    public ConversationMemoryState appendMessage(ConversationMemoryKey key, ConversationMessage message) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        if (message == null) {
            throw new IllegalArgumentException("message must not be null");
        }

        ConversationMemoryState state = store.find(key)
                .orElse(new ConversationMemoryState(
                        key.agentId(),
                        key.conversationId(),
                        new ArrayList<>(),
                        new ArrayList<>(),
                        Instant.now()
                ));

        List<ConversationSummary> summaries = new ArrayList<>(state.summaries());
        List<ConversationMessage> recentMessages = new ArrayList<>(state.recentMessages());
        recentMessages.add(message);

        ConversationMemoryState updated = new ConversationMemoryState(
                key.agentId(),
                key.conversationId(),
                summaries,
                recentMessages,
                Instant.now()
        );

        ConversationMemoryState compacted = compactIfNeeded(updated);
        return store.save(compacted);
    }

    @Override
    public ConversationMemoryState getState(ConversationMemoryKey key) {
        if (key == null) {
            throw new IllegalArgumentException("key must not be null");
        }
        return store.find(key)
                .orElse(new ConversationMemoryState(
                        key.agentId(),
                        key.conversationId(),
                        List.of(),
                        List.of(),
                        Instant.now()
                ));
    }

    @Override
    public String buildContext(ConversationMemoryKey key) {
        return renderer.render(getState(key));
    }

    @Override
    public void clear(ConversationMemoryKey key) {
        store.delete(key);
    }

    private ConversationMemoryState compactIfNeeded(ConversationMemoryState state) {
        boolean tooManyMessages = state.recentMessages().size() > properties.getMaxRecentMessages();
        boolean tooManyCharacters = totalCharacters(state.recentMessages()) > properties.getMaxRecentCharacters();

        if (!tooManyMessages && !tooManyCharacters) {
            return state;
        }

        if (!properties.isAutoSummarize()) {
            return trimOnly(state);
        }

        int keep = Math.min(properties.getMinMessagesToKeep(), state.recentMessages().size());
        int summarizeCount = Math.max(0, state.recentMessages().size() - keep);

        if (summarizeCount == 0) {
            return trimOnly(state);
        }

        List<ConversationMessage> toSummarize = new ArrayList<>(state.recentMessages().subList(0, summarizeCount));
        List<ConversationMessage> toKeep = new ArrayList<>(
                state.recentMessages().subList(summarizeCount, state.recentMessages().size())
        );

        ConversationCompressionReason reason = tooManyMessages
                ? ConversationCompressionReason.MAX_MESSAGES_EXCEEDED
                : ConversationCompressionReason.MAX_CHARACTERS_EXCEEDED;

        ConversationSummary summary = summarizer.summarize(
                state.agentId(),
                state.conversationId(),
                toSummarize,
                reason
        );

        List<ConversationSummary> summaries = new ArrayList<>(state.summaries());
        summaries.add(summary);

        return new ConversationMemoryState(
                state.agentId(),
                state.conversationId(),
                summaries,
                toKeep,
                Instant.now()
        );
    }

    private ConversationMemoryState trimOnly(ConversationMemoryState state) {
        int max = properties.getMaxRecentMessages();
        if (state.recentMessages().size() <= max) {
            return state;
        }

        List<ConversationMessage> kept = new ArrayList<>(
                state.recentMessages().subList(state.recentMessages().size() - max, state.recentMessages().size())
        );

        return new ConversationMemoryState(
                state.agentId(),
                state.conversationId(),
                state.summaries(),
                kept,
                Instant.now()
        );
    }

    private int totalCharacters(List<ConversationMessage> messages) {
        return messages.stream()
                .map(ConversationMessage::content)
                .filter(Objects::nonNull)
                .mapToInt(String::length)
                .sum();
    }

    private void validateProperties(ConversationMemoryProperties props) {
        if (props.getMaxRecentMessages() <= 0) {
            throw new IllegalStateException("maxRecentMessages must be > 0");
        }
        if (props.getMinMessagesToKeep() <= 0) {
            throw new IllegalStateException("minMessagesToKeep must be > 0");
        }
        if (props.getMinMessagesToKeep() > props.getMaxRecentMessages()) {
            throw new IllegalStateException("minMessagesToKeep must be <= maxRecentMessages");
        }
    }
}

