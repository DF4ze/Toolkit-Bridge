package fr.ses10doigts.toolkitbridge.model.dto.llm.provider;

import java.util.Set;

public record ModelInfo(
        String id,
        String displayName,
        Set<LlmCapability> capabilities
) {
    public ModelInfo {
        capabilities = capabilities == null ? Set.of() : Set.copyOf(capabilities);
    }
}