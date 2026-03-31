package fr.ses10doigts.toolkitbridge.memory.conversation.port;

import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMemoryKey;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMemoryState;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMessage;

public interface ConversationMemoryService {

    ConversationMemoryState appendMessage(ConversationMemoryKey key, ConversationMessage message);

    ConversationMemoryState getState(ConversationMemoryKey key);

    String buildContext(ConversationMemoryKey key);

    void clear(ConversationMemoryKey key);
}

