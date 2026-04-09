package fr.ses10doigts.toolkitbridge.memory.context.global.service;

import fr.ses10doigts.toolkitbridge.memory.config.runtime.MemoryRuntimeConfiguration;
import fr.ses10doigts.toolkitbridge.memory.config.runtime.MemoryRuntimeConfigurationResolver;
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

    private final MemoryRuntimeConfigurationResolver runtimeConfigurationResolver;
    private final WorkspaceLayout workspaceLayout;

    private volatile SharedGlobalContextSnapshot cachedSnapshot;

    public MarkdownSharedGlobalContextProvider(
            MemoryRuntimeConfigurationResolver runtimeConfigurationResolver,
            WorkspaceLayout workspaceLayout
    ) {
        this.runtimeConfigurationResolver = runtimeConfigurationResolver;
        this.workspaceLayout = workspaceLayout;
    }

    @Override
    public SharedGlobalContextSnapshot getSharedGlobalContext() {
        MemoryRuntimeConfiguration.GlobalContext configuration = runtimeConfigurationResolver.snapshot().globalContext();
        if (!configuration.enabled()) {
            return new SharedGlobalContextSnapshot("", List.of(), Instant.now());
        }

        if (configuration.loadMode() == MemoryRuntimeConfiguration.GlobalContextLoadMode.ON_DEMAND) {
            return loadSnapshot(configuration);
        }

        SharedGlobalContextSnapshot localSnapshot = cachedSnapshot;
        if (localSnapshot != null && !isExpired(localSnapshot, configuration)) {
            return localSnapshot;
        }

        synchronized (this) {
            localSnapshot = cachedSnapshot;
            if (localSnapshot != null && !isExpired(localSnapshot, configuration)) {
                return localSnapshot;
            }
            SharedGlobalContextSnapshot refreshed = loadSnapshot(configuration);
            cachedSnapshot = refreshed;
            return refreshed;
        }
    }

    @Override
    public void invalidateCache() {
        cachedSnapshot = null;
    }

    private SharedGlobalContextSnapshot loadSnapshot(MemoryRuntimeConfiguration.GlobalContext configuration) {
        try {
            Path root = workspaceLayout.globalContextRoot();
            List<Path> sourceFiles = resolveSourceFiles(root, configuration);
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

    private List<Path> resolveSourceFiles(Path root, MemoryRuntimeConfiguration.GlobalContext configuration) throws IOException {
        if (!configuration.files().isEmpty()) {
            return configuration.files().stream()
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

    private boolean isExpired(SharedGlobalContextSnapshot snapshot, MemoryRuntimeConfiguration.GlobalContext configuration) {
        return snapshot.loadedAt().plus(configuration.cacheRefreshInterval()).isBefore(Instant.now());
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

}
