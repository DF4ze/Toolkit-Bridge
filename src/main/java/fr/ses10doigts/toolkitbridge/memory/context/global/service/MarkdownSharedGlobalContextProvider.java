package fr.ses10doigts.toolkitbridge.memory.context.global.service;

import fr.ses10doigts.toolkitbridge.memory.context.global.config.GlobalContextProperties;
import fr.ses10doigts.toolkitbridge.memory.context.global.model.SharedGlobalContextSnapshot;
import fr.ses10doigts.toolkitbridge.memory.context.global.port.SharedGlobalContextProvider;
import fr.ses10doigts.toolkitbridge.service.workspace.WorkspaceLayout;
import fr.ses10doigts.toolkitbridge.service.workspace.model.WorkspaceArea;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
@Slf4j
public class MarkdownSharedGlobalContextProvider implements SharedGlobalContextProvider {

    private final GlobalContextProperties properties;
    private final WorkspaceLayout workspaceLayout;

    private volatile SharedGlobalContextSnapshot cachedSnapshot;

    public MarkdownSharedGlobalContextProvider(
            GlobalContextProperties properties,
            WorkspaceLayout workspaceLayout
    ) {
        this.properties = properties;
        this.workspaceLayout = workspaceLayout;
        validateProperties(properties);
    }

    @Override
    public SharedGlobalContextSnapshot getSharedGlobalContext() {
        if (!properties.isEnabled()) {
            return new SharedGlobalContextSnapshot("", List.of(), Instant.now());
        }

        if (properties.getLoadMode() == GlobalContextProperties.LoadMode.ON_DEMAND) {
            return loadSnapshot();
        }

        SharedGlobalContextSnapshot localSnapshot = cachedSnapshot;
        if (localSnapshot != null && !isExpired(localSnapshot)) {
            return localSnapshot;
        }

        synchronized (this) {
            localSnapshot = cachedSnapshot;
            if (localSnapshot != null && !isExpired(localSnapshot)) {
                return localSnapshot;
            }
            SharedGlobalContextSnapshot refreshed = loadSnapshot();
            cachedSnapshot = refreshed;
            return refreshed;
        }
    }

    private SharedGlobalContextSnapshot loadSnapshot() {
        try {
            Path root = workspaceLayout.globalContextRoot();
            List<Path> sourceFiles = resolveSourceFiles(root);
            String content = renderContent(sourceFiles);
            List<String> relativeSources = sourceFiles.stream()
                    .map(this::safeRelativize)
                    .toList();
            return new SharedGlobalContextSnapshot(content, relativeSources, Instant.now());
        } catch (IOException | RuntimeException e) {
            log.warn("Unable to load shared global context", e);
            return new SharedGlobalContextSnapshot("", List.of(), Instant.now());
        }
    }

    private List<Path> resolveSourceFiles(Path root) throws IOException {
        if (!properties.getFiles().isEmpty()) {
            return properties.getFiles().stream()
                    .map(this::resolveConfiguredFile)
                    .filter(Files::isRegularFile)
                    .toList();
        }

        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile)
                    .filter(this::isMarkdownFile)
                    .sorted(Comparator.comparing(this::safeRelativize))
                    .toList();
        }
    }

    private Path resolveConfiguredFile(String configuredPath) {
        try {
            Path resolved = workspaceLayout.resolveInArea(
                    WorkspaceArea.GLOBAL_CONTEXT,
                    workspaceLayout.globalContextRoot(),
                    configuredPath
            );
            if (!isMarkdownFile(resolved)) {
                throw new IllegalArgumentException("Configured global context file must be markdown: " + configuredPath);
            }
            if (!Files.exists(resolved)) {
                log.warn("Configured global context file does not exist: {}", configuredPath);
            }
            return resolved;
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to resolve global context file " + configuredPath, e);
        }
    }

    private String renderContent(List<Path> sourceFiles) {
        StringBuilder builder = new StringBuilder();

        for (Path sourceFile : sourceFiles) {
            try {
                String content = Files.readString(sourceFile, StandardCharsets.UTF_8).trim();
                if (content.isBlank()) {
                    continue;
                }
                if (!builder.isEmpty()) {
                    builder.append("\n\n");
                }
                builder.append("### ").append(safeRelativize(sourceFile)).append("\n");
                builder.append(content);
            } catch (IOException e) {
                log.warn("Unable to read global context file {}", sourceFile, e);
            }
        }

        return builder.toString();
    }

    private boolean isExpired(SharedGlobalContextSnapshot snapshot) {
        return snapshot.loadedAt().plus(properties.getCacheRefreshInterval()).isBefore(Instant.now());
    }

    private boolean isMarkdownFile(Path path) {
        return path.getFileName() != null
                && path.getFileName().toString().toLowerCase().endsWith(".md");
    }

    private String safeRelativize(Path path) {
        try {
            return workspaceLayout.relativize(workspaceLayout.globalContextRoot(), path);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to relativize global context file " + path, e);
        }
    }

    private void validateProperties(GlobalContextProperties properties) {
        if (properties.getCacheRefreshInterval().isNegative() || properties.getCacheRefreshInterval().isZero()) {
            throw new IllegalStateException("cacheRefreshInterval must be > 0");
        }
    }
}
