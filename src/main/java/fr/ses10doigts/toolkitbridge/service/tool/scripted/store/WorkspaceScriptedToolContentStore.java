package fr.ses10doigts.toolkitbridge.service.tool.scripted.store;

import fr.ses10doigts.toolkitbridge.service.workspace.WorkspaceLayout;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@Component
public class WorkspaceScriptedToolContentStore implements ScriptedToolContentStore {

    private final WorkspaceLayout workspaceLayout;

    public WorkspaceScriptedToolContentStore(WorkspaceLayout workspaceLayout) {
        this.workspaceLayout = workspaceLayout;
    }

    @Override
    public String save(String toolName, int version, String runtimeType, String content) {
        try {
            Path root = workspaceLayout.scriptedToolsRoot();
            String normalizedToolName = sanitizeSegment(toolName);
            String normalizedRuntime = sanitizeSegment(runtimeType == null || runtimeType.isBlank() ? "shell" : runtimeType);
            Path scriptPath = root
                    .resolve(normalizedToolName)
                    .resolve("v" + version)
                    .resolve("tool." + normalizedRuntime)
                    .normalize();

            if (!scriptPath.startsWith(root)) {
                throw new IllegalArgumentException("Resolved scripted tool path escapes scripted tools root");
            }

            Files.createDirectories(scriptPath.getParent());
            Files.writeString(scriptPath, content == null ? "" : content, StandardCharsets.UTF_8);
            return workspaceLayout.relativize(root, scriptPath);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to persist scripted tool content", e);
        }
    }

    @Override
    public String load(String relativePath) {
        try {
            Path root = workspaceLayout.scriptedToolsRoot();
            Path resolved = workspaceLayout.resolveWithinRoot(root, relativePath, "scripted tools root");
            return Files.readString(resolved, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read scripted tool content", e);
        }
    }

    @Override
    public void delete(String relativePath) {
        try {
            Path root = workspaceLayout.scriptedToolsRoot();
            Path resolved = workspaceLayout.resolveWithinRoot(root, relativePath, "scripted tools root");
            Files.deleteIfExists(resolved);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to delete scripted tool content", e);
        }
    }

    private String sanitizeSegment(String value) {
        String sanitized = value.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("(^-+)|(-+$)", "");
        if (sanitized.isBlank()) {
            throw new IllegalArgumentException("Scripted tool path segment cannot be blank");
        }
        return sanitized;
    }
}
