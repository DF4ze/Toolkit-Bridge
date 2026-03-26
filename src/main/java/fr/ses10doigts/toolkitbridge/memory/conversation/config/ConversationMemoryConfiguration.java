package fr.ses10doigts.toolkitbridge.memory.conversation.config;

import fr.ses10doigts.toolkitbridge.memory.conversation.port.ConversationContextRenderer;
import fr.ses10doigts.toolkitbridge.memory.conversation.port.ConversationMemoryStore;
import fr.ses10doigts.toolkitbridge.memory.conversation.port.ConversationSummarizer;
import fr.ses10doigts.toolkitbridge.memory.conversation.service.DefaultConversationContextRenderer;
import fr.ses10doigts.toolkitbridge.memory.conversation.service.SimpleConversationSummarizer;
import fr.ses10doigts.toolkitbridge.memory.conversation.store.InMemoryConversationMemoryStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
@EnableConfigurationProperties(ConversationMemoryProperties.class)
@ConditionalOnProperty(prefix = "toolkit.memory.conversation", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ConversationMemoryConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ConversationMemoryStore conversationMemoryStore(ConversationMemoryProperties properties) {
        return new InMemoryConversationMemoryStore(Duration.ofMinutes(properties.getExpireAfterMinutes()));
    }

    @Bean
    @ConditionalOnMissingBean
    public ConversationSummarizer conversationSummarizer(ConversationMemoryProperties properties) {
        return new SimpleConversationSummarizer(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public ConversationContextRenderer conversationContextRenderer() {
        return new DefaultConversationContextRenderer();
    }

}

