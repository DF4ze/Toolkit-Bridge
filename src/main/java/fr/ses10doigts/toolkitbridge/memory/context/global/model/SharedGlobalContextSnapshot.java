package fr.ses10doigts.toolkitbridge.memory.context.global.model;

import java.time.Instant;
import java.util.List;

public record SharedGlobalContextSnapshot(
        String content,
        List<String> sourceFiles,
        Instant loadedAt
) {

    public SharedGlobalContextSnapshot {
        content = content == null ? "" : content.trim();
        sourceFiles = sourceFiles == null ? List.of() : List.copyOf(sourceFiles);
        loadedAt = loadedAt == null ? Instant.now() : loadedAt;
    }

    public boolean isEmpty() {
        return content.isBlank();
    }
}
