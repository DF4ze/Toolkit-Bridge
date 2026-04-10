package fr.ses10doigts.toolkitbridge.service.admin;

import fr.ses10doigts.toolkitbridge.model.dto.admin.technical.TechnicalAdminView;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.service.agent.definition.AgentDefinitionService;
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentPolicy;
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentPolicyRegistry;
import fr.ses10doigts.toolkitbridge.service.agent.policy.ResolvedAgentPolicy;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.AgentRuntimeRegistry;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntimeExecutionSnapshot;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentToolAccess;
import fr.ses10doigts.toolkitbridge.service.tool.ToolDescriptor;
import fr.ses10doigts.toolkitbridge.service.tool.ToolRegistryService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminAgentQueryService {

    private final AgentDefinitionService agentDefinitionService;
    private final AgentRuntimeRegistry runtimeRegistry;
    private final AgentPolicyRegistry policyRegistry;
    private final ToolRegistryService toolRegistryService;

    public AdminAgentQueryService(
            AgentDefinitionService agentDefinitionService,
            AgentRuntimeRegistry runtimeRegistry,
            AgentPolicyRegistry policyRegistry,
            ToolRegistryService toolRegistryService
    ) {
        this.agentDefinitionService = agentDefinitionService;
        this.runtimeRegistry = runtimeRegistry;
        this.policyRegistry = policyRegistry;
        this.toolRegistryService = toolRegistryService;
    }

    public List<TechnicalAdminView.AgentItem> listAgents() {
        return agentDefinitionService.findAll().stream()
                .map(this::toAgentItem)
                .toList();
    }

    private TechnicalAdminView.AgentItem toAgentItem(AgentDefinition definition) {
        AgentRuntime runtime = runtimeRegistry.findByAgentId(definition.id()).orElse(null);
        ResolvedAgentPolicy resolvedPolicy = runtime == null
                ? resolvePolicyWithoutRuntime(definition)
                : runtime.policy();

        List<String> exposedTools = runtime == null
                ? resolveExposedToolsFromPolicy(resolvedPolicy)
                : runtime.toolAccess().exposedTools().stream().map(ToolDescriptor::name).toList();

        return new TechnicalAdminView.AgentItem(
                definition.id(),
                definition.name(),
                definition.role().name(),
                definition.orchestratorType().name(),
                definition.llmProvider(),
                definition.model(),
                definition.policyName(),
                definition.toolsEnabled(),
                runtime == null ? null : toRuntimeItem(runtime.state().snapshot()),
                toPolicyItem(resolvedPolicy),
                exposedTools
        );
    }

    private ResolvedAgentPolicy resolvePolicyWithoutRuntime(AgentDefinition definition) {
        // Admin fallback projection: keep this aligned with runtime preparation inputs when no runtime exists.
        AgentPolicy policy = policyRegistry.getRequired(definition.policyName());
        AgentToolAccess availableToolAccess = new AgentToolAccess(
                definition.toolsEnabled(),
                toolRegistryService.getToolNames(),
                toolRegistryService.getToolDescriptors(),
                java.util.List.of()
        );
        return policy.resolve(definition, availableToolAccess);
    }

    private List<String> resolveExposedToolsFromPolicy(ResolvedAgentPolicy policy) {
        if (policy == null || policy.allowedTools().isEmpty()) {
            return List.of();
        }
        Set<String> allowed = policy.allowedTools();
        return toolRegistryService.getToolDescriptors().stream()
                .map(ToolDescriptor::name)
                .filter(name -> allowed.contains(normalize(name)))
                .sorted()
                .toList();
    }

    private TechnicalAdminView.RuntimeItem toRuntimeItem(AgentRuntimeExecutionSnapshot snapshot) {
        return new TechnicalAdminView.RuntimeItem(
                snapshot.availability() == null ? "UNKNOWN" : snapshot.availability().name(),
                snapshot.busy(),
                snapshot.currentTask(),
                snapshot.activeContext(),
                snapshot.traceId(),
                snapshot.channelType(),
                snapshot.channelConversationId(),
                snapshot.startedAt(),
                snapshot.updatedAt()
        );
    }

    private TechnicalAdminView.PolicyItem toPolicyItem(ResolvedAgentPolicy policy) {
        if (policy == null) {
            return null;
        }
        return new TechnicalAdminView.PolicyItem(
                policy.name(),
                policy.allowedTools(),
                policy.accessibleMemoryScopes().stream().map(Enum::name).collect(Collectors.toSet()),
                policy.delegationAllowed(),
                policy.webAccessAllowed(),
                policy.sharedWorkspaceWriteAllowed(),
                policy.scriptedToolExecutionAllowed()
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
