package fr.ses10doigts.toolkitbridge.service.reload;

import fr.ses10doigts.toolkitbridge.service.llm.runtime.LlmProviderRegistryRuntime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class LlmProviderRegistryReloadHandler implements ReloadDomainHandler {

    private final LlmProviderRegistryRuntime llmProviderRegistryRuntime;

    @Override
    public ReloadDomain domain() {
        return ReloadDomain.LLM_PROVIDER_REGISTRY;
    }

    @Override
    public ReloadDomainResult reload() {
        int providerCount = llmProviderRegistryRuntime.reloadFromDatabase();
        return ReloadDomainResult.success(
                domain(),
                "LLM provider registry reloaded from DB providers=" + providerCount
        );
    }
}
