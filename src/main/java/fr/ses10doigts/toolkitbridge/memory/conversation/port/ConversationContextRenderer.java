package fr.ses10doigts.toolkitbridge.memory.conversation.port;

import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMemoryState;

public interface ConversationContextRenderer {

    String render(ConversationMemoryState state);
}

