package fr.ses10doigts.toolkitbridge.memory.conversation.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toolkit.memory.conversation")
@Data
public class ConversationMemoryProperties {

    /**
     * Enable or disable the conversation memory module.
     */
    private boolean enabled = true;

    /**
     * Maximum number of recent messages before compaction.
     */
    private int maxRecentMessages = 20;

    /**
     * Maximum total characters for recent messages before compaction.
     */
    private int maxRecentCharacters = 12_000;

    /**
     * Minimum number of recent messages to keep after compaction.
     */
    private int minMessagesToKeep = 8;

    /**
     * Inactivity duration before expiration in memory.
     */
    private long expireAfterMinutes = 720;

    /**
     * Enables automatic summarization on overflow.
     */
    private boolean autoSummarize = true;

    /**
     * Maximum characters in a simple summary.
     */
    private int maxSummaryCharacters = 2_000;
}

