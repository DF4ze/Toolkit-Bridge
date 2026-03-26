package fr.ses10doigts.toolkitbridge.memory.conversation.service;

import fr.ses10doigts.toolkitbridge.memory.conversation.config.ConversationMemoryProperties;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationCompressionReason;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMessage;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationSummary;
import fr.ses10doigts.toolkitbridge.memory.conversation.port.ConversationSummarizer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class SimpleConversationSummarizer implements ConversationSummarizer {

    private final ConversationMemoryProperties properties;

    public SimpleConversationSummarizer(ConversationMemoryProperties properties) {
        this.properties = properties;
    }

    @Override
    public ConversationSummary summarize(
            String agentId,
            String conversationId,
            List<ConversationMessage> messages,
            ConversationCompressionReason reason
    ) {
        String content = messages.stream()
                .map(message -> "[" + message.role() + "] " + sanitize(message.content()))
                .collect(Collectors.joining("\n"));

        if (content.length() > properties.getMaxSummaryCharacters()) {
            content = content.substring(0, properties.getMaxSummaryCharacters()) + "\n[summary truncated]";
        }

        Instant from = messages.isEmpty() ? Instant.now() : messages.getFirst().createdAt();
        Instant to = messages.isEmpty() ? Instant.now() : messages.getLast().createdAt();

        return new ConversationSummary(
                UUID.randomUUID(),
                agentId,
                conversationId,
                content,
                messages.size(),
                from,
                to,
                Instant.now(),
                reason
        );
    }

    private String sanitize(String content) {
        if (content == null) {
            return "";
        }
        return content.replace("\r", " ").trim();
    }
}

