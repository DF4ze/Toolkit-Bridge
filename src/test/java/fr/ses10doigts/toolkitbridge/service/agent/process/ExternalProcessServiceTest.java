package fr.ses10doigts.toolkitbridge.service.agent.process;

import fr.ses10doigts.toolkitbridge.model.dto.auth.AuthenticatedAgent;
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentPermissionControlService;
import fr.ses10doigts.toolkitbridge.service.agent.process.model.ExternalProcessSnapshot;
import fr.ses10doigts.toolkitbridge.service.agent.process.model.ExternalProcessUpdateRequest;
import fr.ses10doigts.toolkitbridge.service.agent.process.store.ExternalProcessStore;
import fr.ses10doigts.toolkitbridge.service.auth.CurrentAgentService;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExternalProcessServiceTest {

    @Test
    void fallsBackToBundledDefaultWhenWorkspaceOverrideIsMissing() {
        ExternalProcessStore store = mock(ExternalProcessStore.class);
        DefaultExternalProcessCatalog defaultCatalog = mock(DefaultExternalProcessCatalog.class);
        CurrentAgentService currentAgentService = mock(CurrentAgentService.class);
        AgentPermissionControlService permissionControlService = mock(AgentPermissionControlService.class);
        when(store.findById("task-execution-prompt")).thenReturn(Optional.empty());
        when(defaultCatalog.findById("task-execution-prompt")).thenReturn(Optional.of(new ExternalProcessSnapshot(
                "task-execution-prompt",
                "Bundled prompt",
                "application/json",
                "{\"systemPromptTemplate\":\"x\",\"userPromptTemplate\":\"y\"}",
                "abc",
                "external-processes/task-execution-prompt/content.json",
                "external-processes/task-execution-prompt/metadata.json",
                Instant.parse("2026-04-07T00:00:00Z"),
                Instant.parse("2026-04-07T00:00:00Z"),
                "system-bootstrap",
                "initial"
        )));

        ExternalProcessService service = new ExternalProcessService(
                store,
                defaultCatalog,
                new ObjectMapper(),
                currentAgentService,
                permissionControlService
        );

        Optional<ExternalProcessSnapshot> snapshot = service.findById("task-execution-prompt");

        assertThat(snapshot).isPresent();
        assertThat(snapshot.get().description()).isEqualTo("Bundled prompt");
    }

    @Test
    void enforcesPermissionBeforeUpdatingProcess() {
        ExternalProcessStore store = mock(ExternalProcessStore.class);
        DefaultExternalProcessCatalog defaultCatalog = mock(DefaultExternalProcessCatalog.class);
        CurrentAgentService currentAgentService = mock(CurrentAgentService.class);
        AgentPermissionControlService permissionControlService = mock(AgentPermissionControlService.class);
        when(currentAgentService.getCurrentAgent()).thenReturn(new AuthenticatedAgent(UUID.randomUUID(), "agent-1"));
        doNothing().when(permissionControlService).checkSharedWorkspaceWrite("agent-1", "update_external_process:task-execution-prompt");
        when(store.save(org.mockito.ArgumentMatchers.any())).thenAnswer(invocation -> {
            ExternalProcessUpdateRequest request = invocation.getArgument(0);
            return new ExternalProcessSnapshot(
                    request.processId(),
                    request.description(),
                    request.mediaType(),
                    request.content(),
                    "checksum",
                    "processes/" + request.processId() + "/content.json",
                    "processes/" + request.processId() + "/metadata.json",
                    Instant.now(),
                    Instant.now(),
                    request.updatedBy(),
                    request.changeSummary()
            );
        });

        ExternalProcessService service = new ExternalProcessService(
                store,
                defaultCatalog,
                new ObjectMapper(),
                currentAgentService,
                permissionControlService
        );

        service.updateProcess(new ExternalProcessUpdateRequest(
                "task-execution-prompt",
                "Task prompt contract",
                "{\"template\":\"v2\"}",
                "application/json",
                "agent-1",
                "update prompt"
        ));

        verify(permissionControlService).checkSharedWorkspaceWrite("agent-1", "update_external_process:task-execution-prompt");
        verify(store).save(org.mockito.ArgumentMatchers.any());
    }
}
