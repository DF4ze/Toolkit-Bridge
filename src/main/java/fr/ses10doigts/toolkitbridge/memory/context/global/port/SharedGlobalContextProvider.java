package fr.ses10doigts.toolkitbridge.memory.context.global.port;

import fr.ses10doigts.toolkitbridge.memory.context.global.model.SharedGlobalContextSnapshot;

public interface SharedGlobalContextProvider {

    SharedGlobalContextSnapshot getSharedGlobalContext();
}
