package fr.ses10doigts.toolkitbridge.service.agent.runtime.model;

import fr.ses10doigts.toolkitbridge.memory.facade.MemoryFacade;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentRole;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.AgentOrchestrator;
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentPolicy;
import fr.ses10doigts.toolkitbridge.service.agent.policy.ResolvedAgentPolicy;

public record AgentRuntime(
        AgentDefinition definition,
        AgentOrchestrator orchestrator,
        MemoryFacade memory,
        AgentToolAccess toolAccess,
        ResolvedAgentPolicy policy,
        AgentWorkspaceScope workspace,
        AgentRuntimeState state
) {
    public AgentRuntime(
            AgentDefinition definition,
            AgentOrchestrator orchestrator,
            MemoryFacade memory,
            AgentToolAccess toolAccess,
            AgentPolicy policy,
            AgentWorkspaceScope workspace,
            AgentRuntimeState state
    ) {
        this(
                definition,
                orchestrator,
                memory,
                toolAccess,
                policy == null ? null : policy.resolve(definition, toolAccess),
                workspace,
                state
        );
    }

    public String agentId() {
        return definition.id();
    }

    public AgentRole role() {
        return definition.role();
    }
}
