package fr.ses10doigts.toolkitbridge.memory.scoring.service;

import fr.ses10doigts.toolkitbridge.memory.scoring.model.ScorableMemory;

public interface MemoryScoringService {

    double computeScore(ScorableMemory memory);
}
