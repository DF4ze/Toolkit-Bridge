package fr.ses10doigts.toolkitbridge.service.workspace;

import fr.ses10doigts.toolkitbridge.config.workspace.WorkspaceProperties;
import fr.ses10doigts.toolkitbridge.model.dto.auth.AuthenticatedAgent;
import fr.ses10doigts.toolkitbridge.service.auth.CurrentAgentService;
import fr.ses10doigts.toolkitbridge.service.workspace.model.WorkspaceArea;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

@Service
public class WorkspaceService {

    private static final int MAX_COMMAND_ARG_LENGTH = 300;
    private static final String ALLOWED_COMMAND_ARG_PATTERN = "[a-zA-Z0-9._/\\-:=,@+ ]*";

    private final WorkspaceLayout workspaceLayout;
    private final CurrentAgentService currentAgentService;

    public WorkspaceService(
            WorkspaceLayout workspaceLayout,
            CurrentAgentService currentAgentService
    ) throws IOException {
        this.workspaceLayout = workspaceLayout;
        this.currentAgentService = currentAgentService;
        workspaceLayout.agentsRoot();
        workspaceLayout.sharedRoot();
        workspaceLayout.globalContextRoot();
        workspaceLayout.scriptedToolsRoot();
    }

    public Path getCurrentAgentWorkspace() throws IOException {
        AuthenticatedAgent agent = currentAgentService.getCurrentAgent();
        return getAgentWorkspace(agent.agentIdent());
    }

    public Path getAgentWorkspace(String agentIdent) throws IOException {
        return workspaceLayout.agentWorkspace(agentIdent);
    }

    public Path getSharedWorkspace() throws IOException {
        return workspaceLayout.sharedRoot();
    }

    public Path getGlobalContextRoot() throws IOException {
        return workspaceLayout.globalContextRoot();
    }

    public Path getScriptedToolsRoot() throws IOException {
        return workspaceLayout.scriptedToolsRoot();
    }

    public Path resolveInCurrentAgentWorkspace(String userPath) throws IOException {
        return resolveInArea(WorkspaceArea.AGENT_WORKSPACE, userPath);
    }

    public Path resolveInSharedWorkspace(String userPath) throws IOException {
        return resolveInArea(WorkspaceArea.SHARED_WORKSPACE, userPath);
    }

    public Path resolveInGlobalContext(String userPath) throws IOException {
        return resolveInArea(WorkspaceArea.GLOBAL_CONTEXT, userPath);
    }

    public Path resolveInArea(WorkspaceArea area, String userPath) throws IOException {
        Path areaRoot = switch (area) {
            case AGENT_WORKSPACE -> getCurrentAgentWorkspace();
            case SHARED_WORKSPACE, GLOBAL_CONTEXT -> workspaceLayout.areaRoot(area);
        };
        return workspaceLayout.resolveInArea(area, areaRoot, userPath);
    }

    public String relativizeFromCurrentAgentWorkspace(Path path) throws IOException {
        return relativizeFromArea(WorkspaceArea.AGENT_WORKSPACE, path);
    }

    public String relativizeFromSharedWorkspace(Path path) throws IOException {
        return relativizeFromArea(WorkspaceArea.SHARED_WORKSPACE, path);
    }

    public String relativizeFromGlobalContext(Path path) throws IOException {
        return relativizeFromArea(WorkspaceArea.GLOBAL_CONTEXT, path);
    }

    public String relativizeFromArea(WorkspaceArea area, Path path) throws IOException {
        Path areaRoot = switch (area) {
            case AGENT_WORKSPACE -> getCurrentAgentWorkspace();
            case SHARED_WORKSPACE, GLOBAL_CONTEXT -> workspaceLayout.areaRoot(area);
        };
        return workspaceLayout.relativize(areaRoot, path);
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
