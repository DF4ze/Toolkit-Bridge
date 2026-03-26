package fr.ses10doigts.toolkitbridge.memory.conversation.port;

import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMemoryKey;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMemoryState;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMessage;

import java.util.Optional;

public interface ConversationMemoryStore {

    Optional<ConversationMemoryState> find(ConversationMemoryKey key);

    ConversationMemoryState save(ConversationMemoryState state);

    void appendMessage(ConversationMemoryKey key, ConversationMessage message);

    void delete(ConversationMemoryKey key);

    void deleteExpired();
}

