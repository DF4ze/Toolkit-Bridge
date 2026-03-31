package fr.ses10doigts.toolkitbridge.memory.scoring.model;

import java.time.Instant;

public interface ScorableMemory {

    double getImportance();

    int getUsageCount();

    Instant getLastAccessedAt();

    Instant getCreatedAt();
}
