package fr.ses10doigts.toolkitbridge.memory.context.model;

import java.util.List;

public record AssembledContext(
        String text,
        List<Long> injectedSemanticMemoryIds
) {
    public AssembledContext {
        text = text == null ? "" : text;
        injectedSemanticMemoryIds = injectedSemanticMemoryIds == null ? List.of() : List.copyOf(injectedSemanticMemoryIds);
    }
}
