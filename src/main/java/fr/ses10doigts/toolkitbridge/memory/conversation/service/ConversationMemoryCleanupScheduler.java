package fr.ses10doigts.toolkitbridge.memory.conversation.service;

import fr.ses10doigts.toolkitbridge.memory.conversation.config.ConversationMemoryProperties;
import fr.ses10doigts.toolkitbridge.memory.conversation.port.ConversationMemoryStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "toolkit.memory.conversation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ConversationMemoryCleanupScheduler {

    private final ConversationMemoryStore store;
    private final ConversationMemoryProperties properties;

    public ConversationMemoryCleanupScheduler(
            ConversationMemoryStore store,
            ConversationMemoryProperties properties
    ) {
        this.store = store;
        this.properties = properties;
    }

    @Scheduled(fixedDelay = 300_000)
    public void cleanupConversationMemory() {
        if (!properties.isEnabled()) {
            return;
        }
        store.deleteExpired();
    }
}

