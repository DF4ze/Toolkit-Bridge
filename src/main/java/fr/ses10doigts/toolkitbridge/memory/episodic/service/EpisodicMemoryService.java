package fr.ses10doigts.toolkitbridge.memory.episodic.service;

import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeEvent;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeScope;

import java.util.List;

public interface EpisodicMemoryService {

    EpisodeEvent record(EpisodeEvent event);

    List<EpisodeEvent> findRecent(String agentId, int limit);

    List<EpisodeEvent> findRecentByScope(String agentId, EpisodeScope scope, int limit);
}
