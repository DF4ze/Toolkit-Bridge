package fr.ses10doigts.toolkitbridge.memory.scoring.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toolkit.memory.scoring")
@Data
public class MemoryScoringProperties {

    /**
     * Weight applied to usage count in scoring.
     */
    private double usageWeight = 0.5;

}
