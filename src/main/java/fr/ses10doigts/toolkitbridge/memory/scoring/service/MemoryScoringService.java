package fr.ses10doigts.toolkitbridge.memory.scoring.service;

import fr.ses10doigts.toolkitbridge.memory.scoring.model.ScorableMemory;

import java.util.List;

public interface MemoryScoringService {

    double computeScore(ScorableMemory memory);

    <T extends ScorableMemory> List<T> rank(List<T> memories, int limit);
}
