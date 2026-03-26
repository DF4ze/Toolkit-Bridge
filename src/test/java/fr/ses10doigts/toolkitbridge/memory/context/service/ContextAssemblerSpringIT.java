package fr.ses10doigts.toolkitbridge.memory.context.service;

import fr.ses10doigts.toolkitbridge.memory.context.model.ContextRequest;
import fr.ses10doigts.toolkitbridge.memory.context.port.ContextAssembler;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMemoryKey;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMemoryState;
import fr.ses10doigts.toolkitbridge.memory.conversation.model.ConversationMessage;
import fr.ses10doigts.toolkitbridge.memory.conversation.port.ConversationMemoryService;
import fr.ses10doigts.toolkitbridge.memory.retrieval.port.MemoryRetriever;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RuleEntry;
import fr.ses10doigts.toolkitbridge.memory.rule.model.RulePriority;
import fr.ses10doigts.toolkitbridge.memory.rule.service.RuleService;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryScope;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "toolkit.memory.context.max-rules=2",
        "toolkit.memory.context.max-memories=2",
        "toolkit.memory.context.max-characters=500"
})
@Import(ContextAssemblerSpringIT.TestConfig.class)
class ContextAssemblerSpringIT {

    @Autowired
    private ContextAssembler assembler;

    @Test
    void assemblesContextWithProvidedBeans() {
        String context = assembler.buildContext(new ContextRequest(
                "agent-1",
                "conv-1",
                "project-1",
                "User says hello"
        ));

        assertThat(context).contains("## Rules");
        assertThat(context).contains("## Relevant Memories");
        assertThat(context).contains("## Conversation");
        assertThat(context).contains("## User Input");
        assertThat(context).contains("User says hello");
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        public RuleService ruleService() {
            return new RuleService() {
                @Override
                public RuleEntry create(RuleEntry entry) {
                    throw new UnsupportedOperationException("Not used in test");
                }

                @Override
                public RuleEntry update(Long id, RuleEntry updated) {
                    throw new UnsupportedOperationException("Not used in test");
                }

                @Override
                public void activate(Long id) {
                    throw new UnsupportedOperationException("Not used in test");
                }

                @Override
                public void deactivate(Long id) {
                    throw new UnsupportedOperationException("Not used in test");
                }

                @Override
                public List<RuleEntry> getApplicableRules(String agentId, String projectId) {
                    RuleEntry entry = new RuleEntry();
                    entry.setContent("Always be safe");
                    entry.setPriority(RulePriority.HIGH);
                    return List.of(entry);
                }
            };
        }

        @Bean
        @Primary
        public MemoryRetriever memoryRetriever() {
            return query -> {
                MemoryEntry entry = new MemoryEntry();
                entry.setAgentId(query.agentId());
                entry.setScope(MemoryScope.AGENT);
                entry.setType(MemoryType.FACT);
                entry.setContent("Memory content");
                return List.of(entry);
            };
        }

        @Bean
        @Primary
        public ConversationMemoryService conversationMemoryService() {
            return new ConversationMemoryService() {
                @Override
                public ConversationMemoryState appendMessage(ConversationMemoryKey key, ConversationMessage message) {
                    throw new UnsupportedOperationException("Not used in test");
                }

                @Override
                public ConversationMemoryState getState(ConversationMemoryKey key) {
                    return new ConversationMemoryState(
                            key.agentId(),
                            key.conversationId(),
                            List.of(),
                            List.of(),
                            Instant.now()
                    );
                }

                @Override
                public String buildContext(ConversationMemoryKey key) {
                    return "Conversation content";
                }

                @Override
                public void clear(ConversationMemoryKey key) {
                    throw new UnsupportedOperationException("Not used in test");
                }
            };
        }
    }
}
