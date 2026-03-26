package fr.ses10doigts.toolkitbridge.memory.conversation.store;

import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMemoryKey;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMemoryState;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMessage;
import fr.ses10doigts.toolkitbridge.memory.conversation.port.ConversationMemoryStore;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryConversationMemoryStore implements ConversationMemoryStore {

    private final ConcurrentHashMap<ConversationMemoryKey, ConversationMemoryState> states = new ConcurrentHashMap<>();
    private final Duration ttl;

    public InMemoryConversationMemoryStore(Duration ttl) {
        this.ttl = ttl;
    }

    @Override
    public Optional<ConversationMemoryState> find(ConversationMemoryKey key) {
        if (key == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(states.get(key));
    }

    @Override
    public ConversationMemoryState save(ConversationMemoryState state) {
        ConversationMemoryKey key = new ConversationMemoryKey(state.agentId(), state.conversationId());
        states.put(key, state);
        return state;
    }

    @Override
    public void appendMessage(ConversationMemoryKey key, ConversationMessage message) {
        throw new UnsupportedOperationException("Use save() via service orchestration");
    }

    @Override
    public void delete(ConversationMemoryKey key) {
        if (key == null) {
            return;
        }
        states.remove(key);
    }

    @Override
    public void deleteExpired() {
        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            return;
        }
        Instant now = Instant.now();
        states.entrySet().removeIf(entry -> entry.getValue().updatedAt().plus(ttl).isBefore(now));
    }
}

