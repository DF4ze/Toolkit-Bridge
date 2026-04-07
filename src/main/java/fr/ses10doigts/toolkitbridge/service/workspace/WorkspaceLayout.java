package fr.ses10doigts.toolkitbridge.service.workspace;

import fr.ses10doigts.toolkitbridge.config.workspace.WorkspaceProperties;
import fr.ses10doigts.toolkitbridge.exception.ForbiddenCommandException;
import fr.ses10doigts.toolkitbridge.service.workspace.model.WorkspaceArea;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class WorkspaceLayout {

    private final Path agentsRoot;
    private final Path sharedRoot;
    private final Path globalContextRoot;
    private final Path scriptedToolsRoot;
    private final Path externalProcessesRoot;

    public WorkspaceLayout(WorkspaceProperties workspaceProperties) {
        this.agentsRoot = Path.of(workspaceProperties.getAgentsRoot()).normalize();
        this.sharedRoot = Path.of(workspaceProperties.getSharedRoot()).normalize();
        this.globalContextRoot = Path.of(workspaceProperties.getGlobalContextRoot()).normalize();
        this.scriptedToolsRoot = Path.of(workspaceProperties.getScriptedToolsRoot()).normalize();
        this.externalProcessesRoot = Path.of(workspaceProperties.getExternalProcessesRoot()).normalize();
    }

    public Path agentsRoot() throws IOException {
        Files.createDirectories(agentsRoot);
        return agentsRoot;
    }

    public Path sharedRoot() throws IOException {
        Files.createDirectories(sharedRoot);
        return sharedRoot;
    }

    public Path globalContextRoot() throws IOException {
        Files.createDirectories(globalContextRoot);
        return globalContextRoot;
    }

    public Path scriptedToolsRoot() throws IOException {
        Files.createDirectories(scriptedToolsRoot);
        return scriptedToolsRoot;
    }

    public Path externalProcessesRoot() throws IOException {
        Files.createDirectories(externalProcessesRoot);
        return externalProcessesRoot;
    }

    public Path agentWorkspace(String agentIdent) throws IOException {
        String safeAgentFolderName = WorkspacePathSanitizer.sanitizeAgentFolderName(agentIdent);
        Path agentsRoot = agentsRoot();
        Path agentWorkspace = agentsRoot.resolve(safeAgentFolderName).normalize();

        if (!agentWorkspace.startsWith(agentsRoot)) {
            throw new ForbiddenCommandException("Resolved agent workspace escapes agents root");
        }

        Files.createDirectories(agentWorkspace);
        return agentWorkspace;
    }

    public Path areaRoot(WorkspaceArea area) throws IOException {
        return switch (area) {
            case AGENT_WORKSPACE -> throw new IllegalArgumentException("Agent workspace root requires an agent identifier");
            case SHARED_WORKSPACE -> sharedRoot();
            case GLOBAL_CONTEXT -> globalContextRoot();
        };
    }

    public Path resolveInArea(WorkspaceArea area, Path areaRoot, String userPath) {
        if (userPath == null || userPath.isBlank()) {
            throw new IllegalArgumentException("Path cannot be empty");
        }

        Path inputPath = Path.of(userPath);
        if (inputPath.isAbsolute()) {
            throw new ForbiddenCommandException("Absolute paths are not allowed");
        }

        Path resolved = areaRoot.resolve(inputPath).normalize();
        if (!resolved.startsWith(areaRoot)) {
            throw new ForbiddenCommandException("Path escapes " + area.getExternalName());
        }
        return resolved;
    }

    public Path resolveWithinRoot(Path root, String userPath, String rootLabel) {
        if (userPath == null || userPath.isBlank()) {
            throw new IllegalArgumentException("Path cannot be empty");
        }

        Path inputPath = Path.of(userPath);
        if (inputPath.isAbsolute()) {
            throw new ForbiddenCommandException("Absolute paths are not allowed");
        }

        Path resolved = root.resolve(inputPath).normalize();
        if (!resolved.startsWith(root)) {
            throw new ForbiddenCommandException("Path escapes " + rootLabel);
        }
        return resolved;
    }

    public String relativize(Path areaRoot, Path path) {
        return areaRoot.relativize(path)
                .toString()
                .replace("\\", "/");
    }
}
