package fr.ses10doigts.toolkitbridge.service.llm.runtime;

import fr.ses10doigts.toolkitbridge.config.llm.OpenAiLikeProperties;
import fr.ses10doigts.toolkitbridge.service.configuration.admin.AdministrableConfigurationGateway;
import fr.ses10doigts.toolkitbridge.service.configuration.admin.StartupOrder;
import fr.ses10doigts.toolkitbridge.service.llm.provider.LlmProviderRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Order(StartupOrder.LLM_PROVIDER_RUNTIME)
@RequiredArgsConstructor
@Slf4j
public class LlmProviderRegistryRuntime implements ApplicationRunner {

    private final AdministrableConfigurationGateway configurationGateway;
    private final LlmProviderRegistryFactory registryFactory;
    private volatile LlmProviderRegistry snapshot;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        int providerCount = reloadFromDatabase();
        log.info("LLM provider registry initialized from DB providers={}", providerCount);
    }

    public LlmProviderRegistry snapshot() {
        LlmProviderRegistry local = snapshot;
        if (local == null) {
            throw new IllegalStateException("LLM provider registry is not initialized yet");
        }
        return local;
    }

    public synchronized int reloadFromDatabase() {
        List<OpenAiLikeProperties> providers = configurationGateway.loadOpenAiLikeProviders();
        LlmProviderRegistry candidate = registryFactory.build(providers);
        snapshot = candidate;
        return providers == null ? 0 : providers.size();
    }
}
