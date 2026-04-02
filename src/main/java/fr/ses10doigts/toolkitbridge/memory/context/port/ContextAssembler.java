package fr.ses10doigts.toolkitbridge.memory.context.port;

import fr.ses10doigts.toolkitbridge.memory.context.model.AssembledContext;
import fr.ses10doigts.toolkitbridge.memory.context.model.ContextRequest;
import fr.ses10doigts.toolkitbridge.memory.retrieval.model.RetrievedMemories;

public interface ContextAssembler {

    AssembledContext buildContext(ContextRequest request, RetrievedMemories retrievedMemories);
}
