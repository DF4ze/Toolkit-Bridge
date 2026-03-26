package fr.ses10doigts.toolkitbridge.memory.conversation.service;

import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMemoryKey;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMessage;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationRole;
import fr.ses10doigts.toolkitbridge.memory.conversation.port.ConversationMemoryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "toolkit.memory.conversation.max-recent-messages=3",
        "toolkit.memory.conversation.min-messages-to-keep=2",
        "toolkit.memory.conversation.max-recent-characters=1000",
        "toolkit.memory.conversation.auto-summarize=true"
})
class ConversationMemorySpringIT {

    @Autowired
    private ConversationMemoryService service;

    @Test
    void storesMessagesIsolatedByAgentAndConversation() {
        ConversationMemoryKey keyA = new ConversationMemoryKey("agent-1", "conv-1");
        ConversationMemoryKey keyB = new ConversationMemoryKey("agent-2", "conv-1");
        ConversationMemoryKey keyC = new ConversationMemoryKey("agent-1", "conv-2");

        service.appendMessage(keyA, message("agent-1", "conv-1", "hello"));
        service.appendMessage(keyB, message("agent-2", "conv-1", "hola"));
        service.appendMessage(keyC, message("agent-1", "conv-2", "salut"));

        assertThat(service.getState(keyA).recentMessages()).hasSize(1);
        assertThat(service.getState(keyB).recentMessages()).hasSize(1);
        assertThat(service.getState(keyC).recentMessages()).hasSize(1);
    }

    private ConversationMessage message(String agentId, String conversationId, String content) {
        return new ConversationMessage(
                UUID.randomUUID(),
                agentId,
                conversationId,
                ConversationRole.USER,
                content,
                Instant.now(),
                Map.of()
        );
    }
}

