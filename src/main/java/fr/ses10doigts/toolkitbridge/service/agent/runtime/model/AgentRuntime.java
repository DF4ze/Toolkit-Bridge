package fr.ses10doigts.toolkitbridge.service.agent.runtime.model;

import fr.ses10doigts.toolkitbridge.memory.facade.MemoryFacade;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentRole;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.AgentOrchestrator;
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentPolicy;

public record AgentRuntime(
        AgentDefinition definition,
        AgentOrchestrator orchestrator,
        MemoryFacade memory,
        AgentToolAccess toolAccess,
        AgentPolicy policy,
        AgentWorkspaceScope workspace,
        AgentRuntimeState state
) {
    public String agentId() {
        return definition.id();
    }

    public AgentRole role() {
        return definition.role();
    }
}
