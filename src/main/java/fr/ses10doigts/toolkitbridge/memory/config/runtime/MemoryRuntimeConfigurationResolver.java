package fr.ses10doigts.toolkitbridge.memory.config.runtime;

import fr.ses10doigts.toolkitbridge.service.configuration.admin.AdministrableConfigurationGateway;
import fr.ses10doigts.toolkitbridge.service.configuration.admin.StartupOrder;
import fr.ses10doigts.toolkitbridge.service.configuration.admin.payload.MemoryConfigurationPayload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(StartupOrder.MEMORY_RUNTIME_CONFIGURATION)
@RequiredArgsConstructor
@Slf4j
public class MemoryRuntimeConfigurationResolver implements ApplicationRunner {

    private final AdministrableConfigurationGateway configurationGateway;
    private final MemoryRuntimeConfigurationMapper runtimeConfigurationMapper;
    private volatile MemoryRuntimeConfiguration snapshot;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        boolean loadedFromDatabase = reloadFromDatabase();
        if (loadedFromDatabase) {
            log.info("Memory runtime configuration initialized from DB key=memory.configuration");
            return;
        }
        log.warn("Memory runtime configuration missing in DB key=memory.configuration -> internal defaults applied");
    }

    public MemoryRuntimeConfiguration snapshot() {
        MemoryRuntimeConfiguration local = snapshot;
        if (local == null) {
            throw new IllegalStateException("Memory runtime configuration is not initialized yet");
        }
        return local;
    }

    public synchronized boolean reloadFromDatabase() {
        Optional<MemoryConfigurationPayload> payload = configurationGateway.loadMemoryConfiguration();
        MemoryRuntimeConfiguration candidate = runtimeConfigurationMapper.toRuntimeConfiguration(payload.orElse(null));
        validateSnapshot(candidate);
        snapshot = candidate;
        return payload.isPresent();
    }

    private void validateSnapshot(MemoryRuntimeConfiguration configuration) {
        if (configuration.context().maxRules() <= 0
                || configuration.context().maxMemories() <= 0
                || configuration.context().maxCharacters() <= 0
                || configuration.context().maxEpisodes() <= 0) {
            throw new IllegalStateException("Invalid memory runtime context configuration");
        }
        if (configuration.retrieval().maxRules() <= 0
                || configuration.retrieval().maxSemanticMemories() <= 0
                || configuration.retrieval().maxCandidatePoolSize() <= 0
                || configuration.retrieval().maxEpisodes() <= 0
                || configuration.retrieval().maxProjectEpisodeFetch() <= 0
                || configuration.retrieval().conversationSliceMaxCharacters() <= 0) {
            throw new IllegalStateException("Invalid memory runtime retrieval configuration");
        }
        if (configuration.globalContext().cacheRefreshInterval().isNegative()
                || configuration.globalContext().cacheRefreshInterval().isZero()) {
            throw new IllegalStateException("Invalid memory runtime global context configuration");
        }
    }

}
