package fr.ses10doigts.toolkitbridge.memory.scoring.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toolkit.memory.scoring")
@Data
public class MemoryScoringProperties {

    /**
     * Weight applied to the importance signal.
     */
    private double importanceWeight = 1.0;

    /**
     * Weight applied to the frequency signal (usage count).
     */
    private double usageWeight = 0.5;

    /**
     * Weight applied to the recency signal.
     */
    private double recencyWeight = 1.0;

}
