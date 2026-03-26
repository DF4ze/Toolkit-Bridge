package fr.ses10doigts.toolkitbridge.memory.scoring.service;

import fr.ses10doigts.toolkitbridge.memory.scoring.config.MemoryScoringProperties;
import fr.ses10doigts.toolkitbridge.memory.scoring.model.ScorableMemory;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Service
@AllArgsConstructor
public class DefaultMemoryScoringService implements MemoryScoringService {

    private final MemoryScoringProperties properties;
    private final Clock clock;

    @Override
    public double computeScore(ScorableMemory memory) {
        if (memory == null) {
            throw new IllegalArgumentException("memory must not be null");
        }

        double importance = memory.getImportance();
        double usage = memory.getUsageCount() * properties.getUsageWeight();
        double recency = computeRecency(resolveReferenceTime(memory));

        return importance + usage + recency;
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
}
