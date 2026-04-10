package fr.ses10doigts.toolkitbridge.service.admin;

import fr.ses10doigts.toolkitbridge.model.dto.admin.technical.TechnicalAdminView;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentMemoryScope;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentOrchestratorType;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentRole;
import fr.ses10doigts.toolkitbridge.service.agent.definition.AgentDefinitionService;
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentPolicy;
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentPolicyRegistry;
import fr.ses10doigts.toolkitbridge.service.agent.policy.ResolvedAgentPolicy;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.AgentRuntimeRegistry;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentAvailability;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntimeExecutionSnapshot;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntimeState;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentToolAccess;
import fr.ses10doigts.toolkitbridge.service.tool.ToolCapability;
import fr.ses10doigts.toolkitbridge.service.tool.ToolCategory;
import fr.ses10doigts.toolkitbridge.service.tool.ToolDescriptor;
import fr.ses10doigts.toolkitbridge.service.tool.ToolKind;
import fr.ses10doigts.toolkitbridge.service.tool.ToolRegistryService;
import fr.ses10doigts.toolkitbridge.service.tool.ToolRiskLevel;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AdminAgentQueryServiceTest {

    @Test
    void listAgentsUsesRuntimePolicyAndExposedToolsWhenRuntimeExists() {
        AgentDefinitionService definitionService = mock(AgentDefinitionService.class);
        AgentRuntimeRegistry runtimeRegistry = mock(AgentRuntimeRegistry.class);
        AgentPolicyRegistry policyRegistry = mock(AgentPolicyRegistry.class);
        ToolRegistryService toolRegistryService = mock(ToolRegistryService.class);

        AgentDefinition definition = definition("agent-1", AgentRole.ASSISTANT);
        when(definitionService.findAll()).thenReturn(List.of(definition));

        AgentRuntime runtime = mock(AgentRuntime.class);
        ResolvedAgentPolicy runtimePolicy = new ResolvedAgentPolicy(
                "runtime-policy",
                Set.of("read_file"),
                Set.of(AgentMemoryScope.USER),
                true,
                false,
                false,
                true
        );
        AgentRuntimeState runtimeState = mock(AgentRuntimeState.class);
        AgentRuntimeExecutionSnapshot snapshot = new AgentRuntimeExecutionSnapshot(
                AgentAvailability.AVAILABLE,
                true,
                "task-99",
                "context-a",
                "trace-1",
                "telegram",
                "conv-1",
                Instant.parse("2026-01-02T10:00:00Z"),
                Instant.parse("2026-01-02T10:01:00Z")
        );

        when(runtime.policy()).thenReturn(runtimePolicy);
        when(runtime.toolAccess()).thenReturn(new AgentToolAccess(
                true,
                Set.of("unused"),
                List.of(),
                List.of(tool("z_tool"), tool("a_tool"))
        ));
        when(runtime.state()).thenReturn(runtimeState);
        when(runtimeState.snapshot()).thenReturn(snapshot);
        when(runtimeRegistry.findByAgentId("agent-1")).thenReturn(Optional.of(runtime));

        AdminAgentQueryService service = service(
                definitionService,
                runtimeRegistry,
                policyRegistry,
                toolRegistryService
        );

        List<TechnicalAdminView.AgentItem> items = service.listAgents();

        assertThat(items).hasSize(1);
        TechnicalAdminView.AgentItem item = items.get(0);
        assertThat(item.agentId()).isEqualTo("agent-1");
        assertThat(item.provider()).isEqualTo("openai");
        assertThat(item.model()).isEqualTo("gpt-4.1-mini");
        assertThat(item.orchestrator()).isEqualTo("CHAT");
        assertThat(item.policyName()).isEqualTo("default");
        assertThat(item.toolsEnabled()).isTrue();
        assertThat(item.runtime()).isNotNull();
        assertThat(item.runtime().busy()).isTrue();
        assertThat(item.policy()).isNotNull();
        assertThat(item.policy().name()).isEqualTo("runtime-policy");
        assertThat(item.exposedTools()).containsExactly("z_tool", "a_tool");
        assertThat(item.policy().accessibleMemoryScopes()).containsExactly("USER");

        verify(policyRegistry, never()).getRequired(any());
    }

    @Test
    void listAgentsFallsBackToPolicyRegistryAndFiltersAndSortsExposedToolsWhenRuntimeMissing() {
        AgentDefinitionService definitionService = mock(AgentDefinitionService.class);
        AgentRuntimeRegistry runtimeRegistry = mock(AgentRuntimeRegistry.class);
        AgentPolicyRegistry policyRegistry = mock(AgentPolicyRegistry.class);
        ToolRegistryService toolRegistryService = mock(ToolRegistryService.class);
        AgentPolicy policy = mock(AgentPolicy.class);

        AgentDefinition definition = definition("agent-2", AgentRole.EXECUTOR);
        when(definitionService.findAll()).thenReturn(List.of(definition));
        when(runtimeRegistry.findByAgentId("agent-2")).thenReturn(Optional.empty());
        when(policyRegistry.getRequired("default")).thenReturn(policy);
        when(toolRegistryService.getToolNames()).thenReturn(Set.of("write_file", "read_file"));
        when(toolRegistryService.getToolDescriptors()).thenReturn(List.of(tool("write_file"), tool("read_file")));
        when(policy.resolve(eq(definition), any(AgentToolAccess.class))).thenReturn(new ResolvedAgentPolicy(
                "default",
                Set.of("missing_tool", "read_file"),
                Set.of(AgentMemoryScope.PROJECT, AgentMemoryScope.GLOBAL),
                true,
                true,
                false,
                true
        ));

        AdminAgentQueryService service = service(
                definitionService,
                runtimeRegistry,
                policyRegistry,
                toolRegistryService
        );

        List<TechnicalAdminView.AgentItem> items = service.listAgents();

        assertThat(items).hasSize(1);
        TechnicalAdminView.AgentItem item = items.get(0);
        assertThat(item.runtime()).isNull();
        assertThat(item.policy()).isNotNull();
        assertThat(item.policy().name()).isEqualTo("default");
        assertThat(item.exposedTools()).containsExactly("read_file");
        assertThat(item.policy().accessibleMemoryScopes()).containsExactlyInAnyOrder("PROJECT", "GLOBAL");

        verify(policyRegistry).getRequired("default");
        verify(policy).resolve(eq(definition), any(AgentToolAccess.class));
    }

    @Test
    void listAgentsFallbackBuildsPolicyInputAlignedWithRuntimePreparationContract() {
        AgentDefinitionService definitionService = mock(AgentDefinitionService.class);
        AgentRuntimeRegistry runtimeRegistry = mock(AgentRuntimeRegistry.class);
        AgentPolicyRegistry policyRegistry = mock(AgentPolicyRegistry.class);
        ToolRegistryService toolRegistryService = mock(ToolRegistryService.class);
        AgentPolicy policy = mock(AgentPolicy.class);

        AgentDefinition definition = definition("agent-4", AgentRole.ASSISTANT);
        ToolDescriptor read = tool("read_file");
        ToolDescriptor write = tool("write_file");

        when(definitionService.findAll()).thenReturn(List.of(definition));
        when(runtimeRegistry.findByAgentId("agent-4")).thenReturn(Optional.empty());
        when(policyRegistry.getRequired("default")).thenReturn(policy);
        when(toolRegistryService.getToolNames()).thenReturn(Set.of("read_file", "write_file"));
        when(toolRegistryService.getToolDescriptors()).thenReturn(List.of(read, write));
        when(policy.resolve(eq(definition), any(AgentToolAccess.class))).thenReturn(new ResolvedAgentPolicy(
                "default",
                Set.of("read_file"),
                Set.of(AgentMemoryScope.USER),
                true,
                true,
                true,
                true
        ));

        AdminAgentQueryService service = service(
                definitionService,
                runtimeRegistry,
                policyRegistry,
                toolRegistryService
        );

        service.listAgents();

        ArgumentCaptor<AgentToolAccess> toolAccessCaptor = ArgumentCaptor.forClass(AgentToolAccess.class);
        verify(policy).resolve(eq(definition), toolAccessCaptor.capture());
        AgentToolAccess captured = toolAccessCaptor.getValue();
        assertThat(captured.enabled()).isTrue();
        assertThat(captured.allowedTools()).containsExactlyInAnyOrder("read_file", "write_file");
        assertThat(captured.registeredTools()).containsExactly(read, write);
        assertThat(captured.exposedTools()).isEmpty();
    }

    @Test
    void listAgentsMapsRuntimeAvailabilityToUnknownWhenSnapshotAvailabilityIsNull() {
        AgentDefinitionService definitionService = mock(AgentDefinitionService.class);
        AgentRuntimeRegistry runtimeRegistry = mock(AgentRuntimeRegistry.class);

        AgentDefinition definition = definition("agent-3", AgentRole.ASSISTANT);
        when(definitionService.findAll()).thenReturn(List.of(definition));

        AgentRuntime runtime = mock(AgentRuntime.class);
        AgentRuntimeState runtimeState = mock(AgentRuntimeState.class);
        AgentRuntimeExecutionSnapshot snapshot = new AgentRuntimeExecutionSnapshot(
                null,
                false,
                null,
                null,
                null,
                null,
                null,
                Instant.parse("2026-01-03T10:00:00Z"),
                Instant.parse("2026-01-03T10:00:30Z")
        );
        when(runtime.policy()).thenReturn(new ResolvedAgentPolicy(
                "default",
                Set.of(),
                Set.of(),
                true,
                true,
                true,
                true
        ));
        when(runtime.toolAccess()).thenReturn(new AgentToolAccess(true, Set.of(), List.of(), List.of()));
        when(runtime.state()).thenReturn(runtimeState);
        when(runtimeState.snapshot()).thenReturn(snapshot);
        when(runtimeRegistry.findByAgentId("agent-3")).thenReturn(Optional.of(runtime));

        AdminAgentQueryService service = service(
                definitionService,
                runtimeRegistry,
                mock(AgentPolicyRegistry.class),
                mock(ToolRegistryService.class)
        );

        List<TechnicalAdminView.AgentItem> items = service.listAgents();

        assertThat(items).hasSize(1);
        assertThat(items.get(0).runtime()).isNotNull();
        assertThat(items.get(0).runtime().availability()).isEqualTo("UNKNOWN");
    }

    private AdminAgentQueryService service(
            AgentDefinitionService definitionService,
            AgentRuntimeRegistry runtimeRegistry,
            AgentPolicyRegistry policyRegistry,
            ToolRegistryService toolRegistryService
    ) {
        return new AdminAgentQueryService(
                definitionService,
                runtimeRegistry,
                policyRegistry,
                toolRegistryService
        );
    }

    private AgentDefinition definition(String id, AgentRole role) {
        return new AgentDefinition(
                id,
                "Agent " + id,
                "bot-" + id,
                role,
                AgentOrchestratorType.CHAT,
                "openai",
                "gpt-4.1-mini",
                "system",
                "default",
                true
        );
    }

    private ToolDescriptor tool(String name) {
        return new ToolDescriptor(
                name,
                ToolKind.NATIVE,
                ToolCategory.INTERNAL,
                "desc",
                java.util.Map.of(),
                Set.of(ToolCapability.FILE_READ),
                ToolRiskLevel.READ_ONLY
        );
    }
}
