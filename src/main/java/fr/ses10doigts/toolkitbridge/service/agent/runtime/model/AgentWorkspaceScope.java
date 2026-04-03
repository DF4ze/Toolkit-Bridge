package fr.ses10doigts.toolkitbridge.service.agent.runtime.model;

import java.nio.file.Path;

public record AgentWorkspaceScope(
        Path agentWorkspace,
        Path sharedWorkspace
) {
}
