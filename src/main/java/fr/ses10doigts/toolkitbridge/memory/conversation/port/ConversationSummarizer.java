package fr.ses10doigts.toolkitbridge.memory.conversation.port;

import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationCompressionReason;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMessage;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationSummary;

import java.util.List;

public interface ConversationSummarizer {

    ConversationSummary summarize(
            String agentId,
            String conversationId,
            List<ConversationMessage> messages,
            ConversationCompressionReason reason
    );
}

