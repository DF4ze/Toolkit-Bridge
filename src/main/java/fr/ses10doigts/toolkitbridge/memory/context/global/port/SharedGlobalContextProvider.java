package fr.ses10doigts.toolkitbridge.memory.context.global.port;

import fr.ses10doigts.toolkitbridge.memory.context.global.model.SharedGlobalContextSnapshot;

public interface SharedGlobalContextProvider {

    SharedGlobalContextSnapshot getSharedGlobalContext();

    /**
     * Explicit cache invalidation hook used by the runtime reload mechanism.
     * This method is best-effort: implementations should avoid throwing and may
     * keep the default no-op behavior when they do not cache any state.
     * If an implementation still throws, callers must treat it as an
     * invalidation failure, not as a runtime configuration rebuild failure.
     */
    default void invalidateCache() {
    }
}
