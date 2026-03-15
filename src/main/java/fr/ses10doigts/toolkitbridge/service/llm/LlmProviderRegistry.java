package fr.ses10doigts.toolkitbridge.service.llm;

import fr.ses10doigts.toolkitbridge.exception.LlmProviderException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class LlmProviderRegistry {

    private final Map<String, LlmProvider> providersByName;

    public LlmProviderRegistry(List<LlmProvider> providers) {
        Map<String, LlmProvider> map = new LinkedHashMap<>();

        for (LlmProvider provider : providers) {
            String name = provider.getName();

            if (name == null || name.isBlank()) {
                throw new LlmProviderException("A provider has a blank name");
            }

            if (map.containsKey(name)) {
                throw new LlmProviderException("Duplicate provider name: " + name);
            }

            map.put(name, provider);
        }

        this.providersByName = Map.copyOf(map);
    }

    public LlmProvider getRequired(String name) {
        LlmProvider provider = providersByName.get(name);

        if (provider == null) {
            throw new LlmProviderException("Unknown provider: " + name);
        }

        return provider;
    }

    public List<LlmProvider> getAll() {
        return List.copyOf(providersByName.values());
    }

    public boolean exists(String name) {
        return providersByName.containsKey(name);
    }
}