package fr.ses10doigts.toolkitbridge.memory.facade.model;

import fr.ses10doigts.toolkitbridge.persistence.model.EphemeralObject;

import java.util.List;

public record MemoryContext(
        String text,
        List<Long> injectedSemanticMemoryIds
) implements EphemeralObject {
    public MemoryContext {
        text = text == null ? "" : text;
        injectedSemanticMemoryIds = injectedSemanticMemoryIds == null ? List.of() : List.copyOf(injectedSemanticMemoryIds);
    }
}
