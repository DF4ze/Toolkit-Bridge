package fr.ses10doigts.toolkitbridge.memory.facade.model;

import java.util.List;

public record MemoryContext(
        String text,
        List<Long> injectedSemanticMemoryIds
) {
    public MemoryContext {
        text = text == null ? "" : text;
        injectedSemanticMemoryIds = injectedSemanticMemoryIds == null ? List.of() : List.copyOf(injectedSemanticMemoryIds);
    }
}
