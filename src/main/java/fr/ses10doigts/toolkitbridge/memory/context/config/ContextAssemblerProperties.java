package fr.ses10doigts.toolkitbridge.memory.context.config;

import fr.ses10doigts.toolkitbridge.memory.config.runtime.MemoryConfigurationDefaults;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toolkit.memory.context")
@Data
public class ContextAssemblerProperties {

    /**
     * Maximum number of rules injected in the context.
     */
    private int maxRules = MemoryConfigurationDefaults.CONTEXT_MAX_RULES;

    /**
     * Maximum number of semantic memories injected in the context.
     */
    private int maxMemories = MemoryConfigurationDefaults.CONTEXT_MAX_MEMORIES;

    /**
     * Maximum number of characters in the final context.
     */
    private int maxCharacters = MemoryConfigurationDefaults.CONTEXT_MAX_CHARACTERS;

    /**
     * Maximum number of episode summaries in the context.
     */
    private int maxEpisodes = MemoryConfigurationDefaults.CONTEXT_MAX_EPISODES;
}
