package fr.ses10doigts.toolkitbridge.memory.retrieval.config;

import fr.ses10doigts.toolkitbridge.memory.config.runtime.MemoryConfigurationDefaults;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toolkit.memory.retrieval")
@Data
public class MemoryRetrievalProperties {

    /**
     * Maximum number of rules to expose.
     */
    private int maxRules = MemoryConfigurationDefaults.RETRIEVAL_MAX_RULES;

    /**
     * Maximum number of semantic memories to expose.
     */
    private int maxSemanticMemories = MemoryConfigurationDefaults.RETRIEVAL_MAX_SEMANTIC_MEMORIES;

    /**
     * Maximum number of semantic memory candidates fetched before scoring.
     */
    private int maxCandidatePoolSize = MemoryConfigurationDefaults.RETRIEVAL_MAX_CANDIDATE_POOL_SIZE;

    /**
     * Maximum number of episodic events to return.
     */
    private int maxEpisodes = MemoryConfigurationDefaults.RETRIEVAL_MAX_EPISODES;

    /**
     * Maximum number of project-scoped episodic events to fetch per request.
     */
    private int maxProjectEpisodeFetch = MemoryConfigurationDefaults.RETRIEVAL_MAX_PROJECT_EPISODE_FETCH;

    /**
     * Maximum number of characters kept from the conversation slice.
     */
    private int conversationSliceMaxCharacters = MemoryConfigurationDefaults.RETRIEVAL_CONVERSATION_SLICE_MAX_CHARACTERS;
}
