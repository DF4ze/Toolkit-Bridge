package fr.ses10doigts.toolkitbridge.memory.semantic.extractor;

import fr.ses10doigts.toolkitbridge.memory.facade.model.MemoryContextRequest;
import fr.ses10doigts.toolkitbridge.memory.semantic.model.MemoryEntry;

import java.util.List;

public interface SemanticMemoryExtractor {

    List<MemoryEntry> extract(MemoryContextRequest request, String text, String source);
}
