package fr.ses10doigts.toolkitbridge.memory.conversation.service;

import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMemoryState;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMessage;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationSummary;
import fr.ses10doigts.toolkitbridge.memory.conversation.port.ConversationContextRenderer;

public class DefaultConversationContextRenderer implements ConversationContextRenderer {

    @Override
    public String render(ConversationMemoryState state) {
        StringBuilder sb = new StringBuilder();

        if (!state.summaries().isEmpty()) {
            sb.append("## Conversation summary").append("\n");
            for (ConversationSummary summary : state.summaries()) {
                sb.append(summary.content()).append("\n");
            }
            sb.append("\n");
        }

        if (!state.recentMessages().isEmpty()) {
            sb.append("## Recent conversation").append("\n");
            for (ConversationMessage message : state.recentMessages()) {
                sb.append(message.role()).append(": ").append(message.content()).append("\n");
            }
        }

        return sb.toString().trim();
    }
}

