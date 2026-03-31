package fr.ses10doigts.toolkitbridge.memory.context.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toolkit.memory.context")
@Data
public class ContextAssemblerProperties {

    /**
     * Maximum number of rules injected in the context.
     */
    private int maxRules = 10;

    /**
     * Maximum number of semantic memories injected in the context.
     */
    private int maxMemories = 10;

    /**
     * Maximum number of characters in the final context.
     */
    private int maxCharacters = 15000;
}
