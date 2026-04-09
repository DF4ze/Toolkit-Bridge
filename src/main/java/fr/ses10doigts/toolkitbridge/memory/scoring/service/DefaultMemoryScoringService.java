package fr.ses10doigts.toolkitbridge.memory.scoring.service;

import fr.ses10doigts.toolkitbridge.memory.config.runtime.MemoryRuntimeConfigurationResolver;
import fr.ses10doigts.toolkitbridge.memory.scoring.model.ScorableMemory;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
@AllArgsConstructor
public class DefaultMemoryScoringService implements MemoryScoringService {

    private final MemoryRuntimeConfigurationResolver runtimeConfigurationResolver;
    private final Clock clock;

    @Override
    public double computeScore(ScorableMemory memory) {
        if (memory == null) {
            throw new IllegalArgumentException("memory must not be null");
        }

        var scoring = runtimeConfigurationResolver.snapshot().scoring();
        double importance = memory.getImportance() * scoring.importanceWeight();
        double frequency = memory.getUsageCount() * scoring.usageWeight();
        double recency = computeRecency(resolveReferenceTime(memory)) * scoring.recencyWeight();

        return importance + frequency + recency;
    }

    @Override
    public <T extends ScorableMemory> List<T> rank(List<T> memories, int limit) {
        if (memories == null || memories.isEmpty() || limit <= 0) {
            return List.of();
        }

        return memories.stream()
                .filter(memory -> memory != null)
                .sorted(scoreComparator())
                .limit(limit)
                .toList();
    }

    private Instant resolveReferenceTime(ScorableMemory memory) {
        if (memory.getLastAccessedAt() != null) {
            return memory.getLastAccessedAt();
        }
        return memory.getCreatedAt();
    }

    private double computeRecency(Instant referenceTime) {
        if (referenceTime == null) {
            return 0.0;
        }
        Instant now = Instant.now(clock);
        long days = Duration.between(referenceTime, now).toDays();
        if (days < 0) {
            days = 0;
        }
        return 1.0 / (1 + days);
    }

    private <T extends ScorableMemory> Comparator<T> scoreComparator() {
        return Comparator.<T>comparingDouble(this::computeScore)
                .reversed()
                .thenComparing(this::resolveReferenceTime, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ScorableMemory::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(Comparator.comparingInt(ScorableMemory::getUsageCount).reversed())
                .thenComparing(Comparator.comparingDouble(ScorableMemory::getImportance).reversed());
    }
}
