package fr.ses10doigts.toolkitbridge.service.reload;

import fr.ses10doigts.toolkitbridge.memory.config.runtime.MemoryRuntimeConfigurationResolver;
import fr.ses10doigts.toolkitbridge.memory.context.global.port.SharedGlobalContextProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MemoryRuntimeConfigurationReloadHandler implements ReloadDomainHandler {

    private final MemoryRuntimeConfigurationResolver runtimeConfigurationResolver;
    private final SharedGlobalContextProvider sharedGlobalContextProvider;

    @Override
    public ReloadDomain domain() {
        return ReloadDomain.MEMORY_RUNTIME_CONFIGURATION;
    }

    @Override
    public ReloadDomainResult reload() {
        boolean loadedFromDatabase = runtimeConfigurationResolver.reloadFromDatabase();
        String message = loadedFromDatabase
                ? "Memory runtime configuration reloaded from DB"
                : "Memory runtime configuration reloaded with internal defaults (DB payload missing)";
        try {
            sharedGlobalContextProvider.invalidateCache();
        } catch (RuntimeException e) {
            log.warn("Memory runtime reload completed but shared global context cache invalidation failed", e);
            return ReloadDomainResult.success(
                    domain(),
                    message + "; shared global context cache invalidation failed"
            );
        }
        return ReloadDomainResult.success(domain(), message);
    }
}
