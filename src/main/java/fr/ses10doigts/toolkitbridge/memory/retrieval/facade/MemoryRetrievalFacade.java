package fr.ses10doigts.toolkitbridge.memory.retrieval.facade;

import fr.ses10doigts.toolkitbridge.memory.context.model.ContextRequest;
import fr.ses10doigts.toolkitbridge.memory.retrieval.model.RetrievedMemories;

public interface MemoryRetrievalFacade {

    RetrievedMemories retrieve(ContextRequest contextRequest);
}
