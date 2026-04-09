package fr.ses10doigts.toolkitbridge.memory.scoring.config;

import fr.ses10doigts.toolkitbridge.memory.config.runtime.MemoryConfigurationDefaults;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toolkit.memory.scoring")
@Data
public class MemoryScoringProperties {

    /**
     * Weight applied to the importance signal.
     */
    private double importanceWeight = MemoryConfigurationDefaults.SCORING_IMPORTANCE_WEIGHT;

    /**
     * Weight applied to the frequency signal (usage count).
     */
    private double usageWeight = MemoryConfigurationDefaults.SCORING_USAGE_WEIGHT;

    /**
     * Weight applied to the recency signal.
     */
    private double recencyWeight = MemoryConfigurationDefaults.SCORING_RECENCY_WEIGHT;

}
