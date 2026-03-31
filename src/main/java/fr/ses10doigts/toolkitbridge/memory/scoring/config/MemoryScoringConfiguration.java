package fr.ses10doigts.toolkitbridge.memory.scoring.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
@EnableConfigurationProperties(MemoryScoringProperties.class)
public class MemoryScoringConfiguration {

    @Bean
    public Clock memoryScoringClock() {
        return Clock.systemUTC();
    }
}
