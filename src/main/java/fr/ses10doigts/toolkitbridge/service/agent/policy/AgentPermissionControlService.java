package fr.ses10doigts.toolkitbridge.service.agent.policy;

import fr.ses10doigts.toolkitbridge.exception.AgentPermissionDeniedException;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentMemoryScope;
import fr.ses10doigts.toolkitbridge.model.dto.auth.AuthenticatedAgent;
import fr.ses10doigts.toolkitbridge.service.agent.definition.AgentDefinitionService;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentToolAccess;
import fr.ses10doigts.toolkitbridge.service.auth.CurrentAgentService;
import fr.ses10doigts.toolkitbridge.service.tool.ToolRegistryService;
import fr.ses10doigts.toolkitbridge.service.tool.ToolSecurityDescriptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentPermissionControlService {

    private final CurrentAgentService currentAgentService;
    private final AgentDefinitionService agentDefinitionService;
    private final AgentPolicyRegistry agentPolicyRegistry;
    private final ObjectProvider<ToolRegistryService> toolRegistryServiceProvider;

    public boolean canExposeTools(AgentRuntime runtime) {
        if (runtime == null || runtime.policy() == null) {
            return false;
        }
        return runtime.definition().toolsEnabled()
                && runtime.toolAccess().enabled()
                && runtime.policy().allowsAnyTool();
    }

    public void checkToolExecution(String toolName) {
        AuthenticatedAgent authenticatedAgent = currentAgentService.getCurrentAgent();
        checkToolExecution(authenticatedAgent.agentIdent(), toolName);
    }

    public void checkToolExecution(String agentId, String toolName) {
        AgentDefinition definition = definition(agentId);
        ResolvedAgentPolicy policy = resolvePolicy(definition);
        String normalizedToolName = normalize(toolName);

        if (!definition.toolsEnabled()) {
            deny(definition.id(), AgentSensitiveAction.TOOL_EXECUTION, normalizedToolName,
                    "tool subsystem disabled for agent");
        }
        if (!policy.allowsTool(normalizedToolName)) {
            deny(definition.id(), AgentSensitiveAction.TOOL_EXECUTION, normalizedToolName,
                    "tool not allowed by agent policy");
        }

        ToolSecurityDescriptor descriptor = toolRegistryService().getSecurityDescriptor(normalizedToolName);
        if (descriptor.scriptedExecution() && !policy.scriptedToolExecutionAllowed()) {
            deny(definition.id(), AgentSensitiveAction.SCRIPTED_TOOL_EXECUTION, normalizedToolName,
                    "scripted tool execution disabled by agent policy");
        }
        if (descriptor.webAccess() && !policy.webAccessAllowed()) {
            deny(definition.id(), AgentSensitiveAction.WEB_ACCESS, normalizedToolName,
                    "web access disabled by agent policy");
        }
    }

    public void checkMemoryScopeAccess(String agentId, AgentMemoryScope scope, String detail) {
        AgentDefinition definition = definition(agentId);
        ResolvedAgentPolicy policy = resolvePolicy(definition);
        if (!policy.allowsMemoryScope(scope)) {
            deny(definition.id(), AgentSensitiveAction.MEMORY_SCOPE_ACCESS, detail,
                    "memory scope not allowed by agent policy");
        }
    }

    public void checkDelegation(String agentId, String detail) {
        AgentDefinition definition = definition(agentId);
        ResolvedAgentPolicy policy = resolvePolicy(definition);
        if (!policy.delegationAllowed()) {
            deny(definition.id(), AgentSensitiveAction.DELEGATION, detail,
                    "delegation disabled by agent policy");
        }
    }

    public void checkSharedWorkspaceWrite(String agentId, String detail) {
        AgentDefinition definition = definition(agentId);
        ResolvedAgentPolicy policy = resolvePolicy(definition);
        if (!policy.sharedWorkspaceWriteAllowed()) {
            deny(definition.id(), AgentSensitiveAction.SHARED_WORKSPACE_WRITE, detail,
                    "shared workspace write disabled by agent policy");
        }
    }

    private void deny(String agentId, AgentSensitiveAction action, String detail, String reason) {
        log.warn("Permission denied agentId={} action={} detail={} reason={}",
                agentId,
                action,
                detail,
                reason);
        throw new AgentPermissionDeniedException(agentId, action, detail);
    }

    private String normalize(String toolName) {
        return toolName == null ? "" : toolName.trim().toLowerCase(java.util.Locale.ROOT);
    }

    private AgentDefinition definition(String agentId) {
        return agentDefinitionService.findById(agentId)
                .orElseThrow(() -> new IllegalStateException("No agent definition found for agentId=" + agentId));
    }

    private ResolvedAgentPolicy resolvePolicy(AgentDefinition definition) {
        AgentPolicy policy = agentPolicyRegistry.getRequired(definition.policyName());
        return policy.resolve(
                definition,
                new AgentToolAccess(definition.toolsEnabled(), toolRegistryService().getToolNames())
        );
    }

    private ToolRegistryService toolRegistryService() {
        ToolRegistryService toolRegistryService = toolRegistryServiceProvider.getIfAvailable();
        if (toolRegistryService == null) {
            throw new IllegalStateException("Tool registry service is unavailable");
        }
        return toolRegistryService;
    }
}
