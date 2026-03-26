package fr.ses10doigts.toolkitbridge.memory.conversation.store;

import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMemoryKey;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMemoryState;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryConversationMemoryStoreTest {

    @Test
    void deleteExpiredRemovesStaleEntries() {
        InMemoryConversationMemoryStore store = new InMemoryConversationMemoryStore(Duration.ofMinutes(1));

        ConversationMemoryState stale = new ConversationMemoryState(
                "agent-1",
                "conv-1",
                List.of(),
                List.of(),
                Instant.now().minus(Duration.ofMinutes(2))
        );

        store.save(stale);

        store.deleteExpired();

        assertThat(store.find(new ConversationMemoryKey("agent-1", "conv-1"))).isEmpty();
    }
}

