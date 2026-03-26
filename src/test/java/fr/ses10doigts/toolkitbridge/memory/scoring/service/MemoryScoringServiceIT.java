package fr.ses10doigts.toolkitbridge.memory.scoring.service;

import fr.ses10doigts.toolkitbridge.memory.scoring.model.ScorableMemory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "toolkit.memory.scoring.usage-weight=1.0")
@Import(MemoryScoringServiceIT.FixedClockConfig.class)
class MemoryScoringServiceIT {

    private static final Instant NOW = Instant.parse("2026-03-26T00:00:00Z");

    @Autowired
    private MemoryScoringService scoringService;

    @Test
    void computeScoreUsesConfiguredWeight() {
        ScorableMemory memory = new TestMemory(1.0, 2, NOW.minusSeconds(86400L * 2), NOW.minusSeconds(86400L * 10));

        double score = scoringService.computeScore(memory);

        double expectedRecency = 1.0 / (1 + 2);
        double expected = 1.0 + (2 * 1.0) + expectedRecency;

        assertThat(score).isEqualTo(expected);
    }

    @TestConfiguration
    static class FixedClockConfig {

        @Bean
        @Primary
        public Clock memoryScoringClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
        }
    }

    private record TestMemory(double importance, int usageCount, Instant lastAccessedAt, Instant createdAt)
            implements ScorableMemory {
        @Override
        public double getImportance() {
            return importance;
        }

        @Override
        public int getUsageCount() {
            return usageCount;
        }

        @Override
        public Instant getLastAccessedAt() {
            return lastAccessedAt;
        }

        @Override
        public Instant getCreatedAt() {
            return createdAt;
        }
    }
}
