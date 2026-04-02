package fr.ses10doigts.toolkitbridge.memory.episodic.factory;

import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeEvent;
import fr.ses10doigts.toolkitbridge.memory.facade.model.MemoryContextRequest;
import fr.ses10doigts.toolkitbridge.memory.facade.model.ToolExecutionRecord;

public interface EpisodicEventFactory {

    EpisodeEvent userMessageReceived(MemoryContextRequest request);

    EpisodeEvent assistantResponseGenerated(MemoryContextRequest request, String assistantMessage);

    EpisodeEvent toolExecutionEvent(MemoryContextRequest request, ToolExecutionRecord toolExecutionRecord);

    EpisodeEvent orchestrationFallback(MemoryContextRequest request, String reason);
}
