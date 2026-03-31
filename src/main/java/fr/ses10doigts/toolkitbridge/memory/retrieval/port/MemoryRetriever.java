package fr.ses10doigts.toolkitbridge.memory.retrieval.port;

import fr.ses10doigts.toolkitbridge.memory.retrieval.model.MemoryQuery;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;

import java.util.List;

public interface MemoryRetriever {

    List<MemoryEntry> retrieve(MemoryQuery query);
}
