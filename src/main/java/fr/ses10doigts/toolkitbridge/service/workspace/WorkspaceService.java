package fr.ses10doigts.toolkitbridge.service.workspace;

import fr.ses10doigts.toolkitbridge.config.workspace.WorkspaceProperties;
import fr.ses10doigts.toolkitbridge.exception.ForbiddenCommandException;
import fr.ses10doigts.toolkitbridge.model.dto.auth.AuthenticatedAgent;
import fr.ses10doigts.toolkitbridge.service.auth.CurrentAgentService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Service
public class WorkspaceService {

    private static final int MAX_COMMAND_ARG_LENGTH = 300;
    private static final String ALLOWED_COMMAND_ARG_PATTERN = "[a-zA-Z0-9._/\\-:=,@+ ]*";

    @Getter
    private final Path agentsRoot;
    @Getter
    private final Path sharedRoot;
    private final CurrentAgentService currentAgentService;

    public WorkspaceService(
            WorkspaceProperties workspaceProperties,
            CurrentAgentService currentAgentService
    ) throws IOException {
        this.agentsRoot = Path.of(workspaceProperties.getAgentsRoot()).normalize();
        this.sharedRoot = Path.of(workspaceProperties.getSharedRoot()).normalize();
        this.currentAgentService = currentAgentService;
        Files.createDirectories(this.agentsRoot);
    }

    public Path getCurrentAgentWorkspace() throws IOException {
        AuthenticatedAgent agent = currentAgentService.getCurrentAgent();
        String safeAgentFolderName = WorkspacePathSanitizer.sanitizeAgentFolderName(agent.agentIdent());

        Path agentWorkspace = agentsRoot.resolve(safeAgentFolderName).normalize();

        if (!agentWorkspace.startsWith(agentsRoot)) {
            throw new ForbiddenCommandException("Resolved agent workspace escapes agents root");
        }

        Files.createDirectories(agentWorkspace);
        return agentWorkspace;
    }

    public Path getSharedWorkspace() throws IOException {
        Files.createDirectories(sharedRoot);
        return sharedRoot;
    }

    public Path resolveInCurrentAgentWorkspace(String userPath) throws IOException {
        if (userPath == null || userPath.isBlank()) {
            throw new IllegalArgumentException("Path cannot be empty");
        }

        Path inputPath = Path.of(userPath);

        if (inputPath.isAbsolute()) {
            throw new ForbiddenCommandException("Absolute paths are not allowed");
        }

        Path agentWorkspace = getCurrentAgentWorkspace();
        Path resolved = agentWorkspace.resolve(inputPath).normalize();

        if (!resolved.startsWith(agentWorkspace)) {
            throw new ForbiddenCommandException("Path escapes bot workspace");
        }

        return resolved;
    }

    public Path resolveInSharedWorkspace(String userPath) throws IOException {
        if (userPath == null || userPath.isBlank()) {
            throw new IllegalArgumentException("Path cannot be empty");
        }

        Path inputPath = Path.of(userPath);
        if (inputPath.isAbsolute()) {
            throw new ForbiddenCommandException("Absolute paths are not allowed");
        }

        Path sharedWorkspace = getSharedWorkspace();
        Path resolved = sharedWorkspace.resolve(inputPath).normalize();

        if (!resolved.startsWith(sharedWorkspace)) {
            throw new ForbiddenCommandException("Path escapes shared workspace");
        }

        return resolved;
    }

    public String relativizeFromCurrentAgentWorkspace(Path path) throws IOException {
        return getCurrentAgentWorkspace()
                .relativize(path)
                .toString()
                .replace("\\", "/");
    }

    public void validateCommandArg(String arg) {
        if (arg == null) {
            throw new IllegalArgumentException("Argument cannot be null");
        }

        if (arg.length() > MAX_COMMAND_ARG_LENGTH) {
            throw new IllegalArgumentException("Argument too long");
        }

        if (arg.matches(".*[\\r\\n\\x00].*")) {
            throw new IllegalArgumentException("Invalid control characters in argument");
        }

        if (!arg.matches(ALLOWED_COMMAND_ARG_PATTERN)) {
            throw new IllegalArgumentException("Forbidden characters in argument: " + arg);
        }
    }

    public Path validateRelativeWorkspacePathArg(String arg) throws IOException {
        validateCommandArg(arg);
        return resolveInCurrentAgentWorkspace(arg);
    }

}
