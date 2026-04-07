package fr.ses10doigts.toolkitbridge.service.agent.runtime;

import fr.ses10doigts.toolkitbridge.memory.facade.MemoryFacade;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.auth.AuthenticatedAgent;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.AgentOrchestrator;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.AgentOrchestratorResolver;
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentPolicy;
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentPolicyRegistry;
import fr.ses10doigts.toolkitbridge.service.agent.policy.ResolvedAgentPolicy;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentToolAccess;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentWorkspaceScope;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntimeState;
import fr.ses10doigts.toolkitbridge.service.tool.ToolRegistryService;
import fr.ses10doigts.toolkitbridge.service.workspace.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class AgentRuntimeFactory {

    private final AgentOrchestratorResolver orchestratorResolver;
    private final AgentPolicyRegistry policyRegistry;
    private final MemoryFacade memoryFacade;
    private final ToolRegistryService toolRegistryService;
    private final WorkspaceService workspaceService;

    public AgentRuntime create(AgentDefinition definition, AuthenticatedAgent authenticatedAgent) {
        AgentOrchestrator orchestrator = orchestratorResolver.resolve(definition);
        AgentPolicy policyResolver = policyRegistry.getRequired(definition.policyName());

        AgentToolAccess toolAccess = new AgentToolAccess(
                definition.toolsEnabled(),
                toolRegistryService.getToolNames()
        );
        ResolvedAgentPolicy policy = policyResolver.resolve(definition, toolAccess);

        AgentWorkspaceScope workspace = resolveWorkspace(authenticatedAgent);

        return new AgentRuntime(
                definition,
                orchestrator,
                memoryFacade,
                toolAccess,
                policy,
                workspace,
                new AgentRuntimeState()
        );
    }

    private AgentWorkspaceScope resolveWorkspace(AuthenticatedAgent authenticatedAgent) {
        try {
            return new AgentWorkspaceScope(
                    workspaceService.getAgentWorkspace(authenticatedAgent.agentIdent()),
                    workspaceService.getSharedWorkspace()
            );
        } catch (IOException e) {
            throw new IllegalStateException("Unable to resolve workspace for runtime", e);
        }
    }
}
