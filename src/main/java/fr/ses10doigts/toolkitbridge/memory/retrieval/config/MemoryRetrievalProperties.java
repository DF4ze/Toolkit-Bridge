package fr.ses10doigts.toolkitbridge.memory.retrieval.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toolkit.memory.retrieval")
@Data
public class MemoryRetrievalProperties {

    /**
     * Maximum number of rules to expose.
     */
    private int maxRules = 10;

    /**
     * Maximum number of semantic memories to expose.
     */
    private int maxSemanticMemories = 10;

    /**
     * Maximum number of episodic events to return.
     */
    private int maxEpisodes = 5;

    /**
     * Maximum number of characters kept from the conversation slice.
     */
    private int conversationSliceMaxCharacters = 4000;
}
