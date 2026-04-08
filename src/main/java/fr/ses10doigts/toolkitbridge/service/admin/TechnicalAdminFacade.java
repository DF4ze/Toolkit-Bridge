package fr.ses10doigts.toolkitbridge.service.admin;

import fr.ses10doigts.toolkitbridge.config.llm.OpenAiLikeProperties;
import fr.ses10doigts.toolkitbridge.model.dto.admin.technical.TechnicalAdminView;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.persistence.model.PersistableObjectFamily;
import fr.ses10doigts.toolkitbridge.persistence.retention.PersistenceRetentionPolicyResolver;
import fr.ses10doigts.toolkitbridge.persistence.retention.RetentionPolicy;
import fr.ses10doigts.toolkitbridge.service.admin.config.AdminTechnicalProperties;
import fr.ses10doigts.toolkitbridge.service.admin.task.AdminTaskSnapshot;
import fr.ses10doigts.toolkitbridge.service.admin.task.AdminTaskStore;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.Artifact;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.service.ArtifactService;
import fr.ses10doigts.toolkitbridge.service.agent.definition.AgentDefinitionService;
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentPolicy;
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentPolicyRegistry;
import fr.ses10doigts.toolkitbridge.service.agent.policy.ResolvedAgentPolicy;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.AgentRuntimeRegistry;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntimeExecutionSnapshot;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentToolAccess;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskStatus;
import fr.ses10doigts.toolkitbridge.service.agent.trace.AgentTraceQueryService;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEvent;
import fr.ses10doigts.toolkitbridge.service.configuration.admin.AdministrableConfigurationGateway;
import fr.ses10doigts.toolkitbridge.service.tool.ToolDescriptor;
import fr.ses10doigts.toolkitbridge.service.tool.ToolRegistryService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TechnicalAdminFacade {

    private final AgentDefinitionService agentDefinitionService;
    private final AgentRuntimeRegistry runtimeRegistry;
    private final AgentPolicyRegistry policyRegistry;
    private final ToolRegistryService toolRegistryService;
    private final AgentTraceQueryService traceQueryService;
    private final AdminTaskStore taskStore;
    private final ArtifactService artifactService;
    private final AdministrableConfigurationGateway configurationGateway;
    private final PersistenceRetentionPolicyResolver retentionPolicyResolver;
    private final AdminTechnicalProperties technicalProperties;

    public TechnicalAdminFacade(
            AgentDefinitionService agentDefinitionService,
            AgentRuntimeRegistry runtimeRegistry,
            AgentPolicyRegistry policyRegistry,
            ToolRegistryService toolRegistryService,
            AgentTraceQueryService traceQueryService,
            AdminTaskStore taskStore,
            ArtifactService artifactService,
            AdministrableConfigurationGateway configurationGateway,
            PersistenceRetentionPolicyResolver retentionPolicyResolver,
            AdminTechnicalProperties technicalProperties
    ) {
        this.agentDefinitionService = agentDefinitionService;
        this.runtimeRegistry = runtimeRegistry;
        this.policyRegistry = policyRegistry;
        this.toolRegistryService = toolRegistryService;
        this.traceQueryService = traceQueryService;
        this.taskStore = taskStore;
        this.artifactService = artifactService;
        this.configurationGateway = configurationGateway;
        this.retentionPolicyResolver = retentionPolicyResolver;
        this.technicalProperties = technicalProperties;
    }

    public List<TechnicalAdminView.AgentItem> listAgents() {
        return agentDefinitionService.findAll().stream()
                .map(this::toAgentItem)
                .toList();
    }

    public List<TechnicalAdminView.TaskItem> listRecentTasks(Integer limit, String agentId, TaskStatus status) {
        int effectiveLimit = technicalProperties.sanitizeLimit(limit);
        return taskStore.recent(effectiveLimit, agentId, status).stream()
                .map(this::toTaskItem)
                .toList();
    }

    public List<TechnicalAdminView.TraceItem> listRecentTraces(Integer limit, String agentId) {
        int effectiveLimit = technicalProperties.sanitizeLimit(limit);
        List<AgentTraceEvent> source = isBlank(agentId)
                ? traceQueryService.recentEvents()
                : traceQueryService.recentEventsForAgent(agentId);

        return source.stream()
                .sorted(Comparator.comparing(AgentTraceEvent::occurredAt).reversed())
                .limit(effectiveLimit)
                .map(this::toTraceItem)
                .toList();
    }

    public List<TechnicalAdminView.ArtifactItem> listRecentArtifacts(Integer limit, String agentId, String taskId) {
        int effectiveLimit = technicalProperties.sanitizeLimit(limit);
        List<Artifact> artifacts;

        if (!isBlank(taskId)) {
            artifacts = artifactService.findByTaskId(taskId);
        } else if (!isBlank(agentId)) {
            artifacts = artifactService.findByProducerAgentId(agentId, effectiveLimit);
        } else {
            artifacts = artifactService.findRecent(effectiveLimit);
        }

        return artifacts.stream()
                .sorted(Comparator.comparing(Artifact::createdAt).reversed())
                .limit(effectiveLimit)
                .map(this::toArtifactItem)
                .toList();
    }

    public TechnicalAdminView.ConfigItem getConfigurationView() {
        List<AgentDefinition> definitions = agentDefinitionService.findAll();
        List<OpenAiLikeProperties> providers = configurationGateway.loadOpenAiLikeProviders();

        List<TechnicalAdminView.LlmProviderItem> providerItems = providers.stream()
                .map(provider -> new TechnicalAdminView.LlmProviderItem(
                        provider.name(),
                        provider.baseUrl(),
                        provider.defaultModel(),
                        !isBlank(provider.apiKey())
                ))
                .toList();

        return new TechnicalAdminView.ConfigItem(
                definitions.size(),
                providerItems.size(),
                providerItems
        );
    }

    public List<TechnicalAdminView.RetentionItem> listRetentionPolicies() {
        return java.util.Arrays.stream(PersistableObjectFamily.values())
                .map(retentionPolicyResolver::resolve)
                .map(this::toRetentionItem)
                .toList();
    }

    public TechnicalAdminView.Overview getOverview(Integer limit) {
        int effectiveLimit = technicalProperties.sanitizeLimit(limit);
        List<TechnicalAdminView.AgentItem> agents = listAgents();
        List<TechnicalAdminView.TaskItem> tasks = listRecentTasks(effectiveLimit, null, null);
        List<TechnicalAdminView.TraceItem> traces = listRecentTraces(effectiveLimit, null);
        List<TechnicalAdminView.ArtifactItem> artifacts = listRecentArtifacts(effectiveLimit, null, null);

        int busyAgents = (int) agents.stream()
                .filter(agent -> agent.runtime() != null && agent.runtime().busy())
                .count();

        int recentErrors = (int) traces.stream()
                .filter(trace -> trace.type() != null && trace.type().name().equals("ERROR"))
                .count();

        return new TechnicalAdminView.Overview(
                Instant.now(),
                agents.size(),
                busyAgents,
                tasks.size(),
                recentErrors,
                traces.size(),
                artifacts.size(),
                getConfigurationView(),
                listRetentionPolicies(),
                agents,
                tasks,
                traces,
                artifacts
        );
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

    private TechnicalAdminView.TaskItem toTaskItem(AdminTaskSnapshot task) {
        return new TechnicalAdminView.TaskItem(
                task.taskId(),
                task.parentTaskId(),
                task.objective(),
                task.initiator(),
                task.assignedAgentId(),
                task.traceId(),
                task.entryPoint() == null ? null : task.entryPoint().name(),
                task.status(),
                task.channelType(),
                task.conversationId(),
                task.firstSeenAt(),
                task.lastSeenAt(),
                task.errorMessage(),
                task.artifactCount()
        );
    }

    private TechnicalAdminView.TraceItem toTraceItem(AgentTraceEvent trace) {
        return new TechnicalAdminView.TraceItem(
                trace.occurredAt(),
                trace.type(),
                trace.source(),
                trace.correlation() == null ? null : trace.correlation().runId(),
                trace.correlation() == null ? null : trace.correlation().agentId(),
                trace.correlation() == null ? null : trace.correlation().messageId(),
                trace.correlation() == null ? null : trace.correlation().taskId(),
                trace.attributes()
        );
    }

    private TechnicalAdminView.ArtifactItem toArtifactItem(Artifact artifact) {
        return new TechnicalAdminView.ArtifactItem(
                artifact.artifactId(),
                artifact.type() == null ? null : artifact.type().name(),
                artifact.taskId(),
                artifact.producerAgentId(),
                artifact.title(),
                artifact.createdAt(),
                artifact.expiresAt(),
                artifact.contentPointer() == null ? null : artifact.contentPointer().storageKind(),
                artifact.contentPointer() == null ? null : artifact.contentPointer().location(),
                artifact.contentPointer() == null ? null : artifact.contentPointer().mediaType(),
                artifact.contentPointer() == null ? 0 : artifact.contentPointer().sizeBytes()
        );
    }

    private TechnicalAdminView.RetentionItem toRetentionItem(RetentionPolicy policy) {
        return new TechnicalAdminView.RetentionItem(
                policy.family().name(),
                policy.ttl(),
                policy.disposition().name()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
