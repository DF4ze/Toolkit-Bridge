package fr.ses10doigts.toolkitbridge.memory.context.port;

import fr.ses10doigts.toolkitbridge.memory.context.model.ContextRequest;

public interface ContextAssembler {

    String buildContext(ContextRequest request);
}
