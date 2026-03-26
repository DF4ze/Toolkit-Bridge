package fr.ses10doigts.toolkitbridge.memory.scoring.service;

import fr.ses10doigts.toolkitbridge.memory.scoring.config.MemoryScoringProperties;
import fr.ses10doigts.toolkitbridge.memory.scoring.model.ScorableMemory;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class DefaultMemoryScoringServiceTest {

    private static final Instant NOW = Instant.parse("2026-03-26T00:00:00Z");

    @Test
    void higherImportanceScoresHigher() {
        DefaultMemoryScoringService service = createService(0.5);

        ScorableMemory low = new TestMemory(0.2, 0, NOW, NOW);
        ScorableMemory high = new TestMemory(0.9, 0, NOW, NOW);

        assertThat(service.computeScore(high)).isGreaterThan(service.computeScore(low));
    }

    @Test
    void higherUsageScoresHigher() {
        DefaultMemoryScoringService service = createService(0.5);

        ScorableMemory low = new TestMemory(0.5, 1, NOW, NOW);
        ScorableMemory high = new TestMemory(0.5, 4, NOW, NOW);

        assertThat(service.computeScore(high)).isGreaterThan(service.computeScore(low));
    }

    @Test
    void moreRecentScoresHigher() {
        DefaultMemoryScoringService service = createService(0.5);

        ScorableMemory recent = new TestMemory(0.5, 0, NOW, NOW.minusSeconds(86400L * 10));
        ScorableMemory old = new TestMemory(0.5, 0, NOW.minusSeconds(86400L * 10), NOW.minusSeconds(86400L * 10));

        assertThat(service.computeScore(recent)).isGreaterThan(service.computeScore(old));
    }

    @Test
    void scoreDeterministicForFixedClock() {
        DefaultMemoryScoringService service = createService(0.5);

        ScorableMemory memory = new TestMemory(0.7, 2, NOW.minusSeconds(86400L * 3), NOW.minusSeconds(86400L * 5));

        double first = service.computeScore(memory);
        double second = service.computeScore(memory);

        assertThat(second).isEqualTo(first);
    }

    @Test
    void usesCreatedAtWhenLastAccessedMissing() {
        DefaultMemoryScoringService service = createService(1.0);

        ScorableMemory memory = new TestMemory(1.0, 0, null, NOW.minusSeconds(86400L * 4));

        double score = service.computeScore(memory);

        assertThat(score).isGreaterThan(1.0);
    }

    @Test
    void rejectNullMemory() {
        DefaultMemoryScoringService service = createService(0.5);

        assertThatThrownBy(() -> service.computeScore(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("memory");
    }

    private DefaultMemoryScoringService createService(double usageWeight) {
        MemoryScoringProperties properties = new MemoryScoringProperties();
        properties.setUsageWeight(usageWeight);
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        return new DefaultMemoryScoringService(properties, clock);
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
