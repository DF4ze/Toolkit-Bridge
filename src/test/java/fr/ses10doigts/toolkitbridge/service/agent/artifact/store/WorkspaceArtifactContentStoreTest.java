package fr.ses10doigts.toolkitbridge.service.agent.artifact.store;

import fr.ses10doigts.toolkitbridge.service.agent.artifact.config.ArtifactStorageProperties;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactContentPointer;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactType;
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentPermissionControlService;
import fr.ses10doigts.toolkitbridge.service.auth.CurrentAgentService;
import fr.ses10doigts.toolkitbridge.model.dto.auth.AuthenticatedAgent;
import fr.ses10doigts.toolkitbridge.service.workspace.WorkspaceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

class WorkspaceArtifactContentStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void storesArtifactPayloadInSharedWorkspace() throws Exception {
        WorkspaceService workspaceService = mock(WorkspaceService.class);
        CurrentAgentService currentAgentService = mock(CurrentAgentService.class);
        AgentPermissionControlService permissionControlService = mock(AgentPermissionControlService.class);
        when(workspaceService.getSharedWorkspace()).thenReturn(tempDir.resolve("shared"));
        when(currentAgentService.getCurrentAgent()).thenReturn(new AuthenticatedAgent(UUID.randomUUID(), "agent-1"));
        doNothing().when(permissionControlService).checkSharedWorkspaceWrite("agent-1", "store_artifact:report");

        ArtifactStorageProperties properties = new ArtifactStorageProperties();
        properties.setContentFolder("artifacts");

        WorkspaceArtifactContentStore store = new WorkspaceArtifactContentStore(
                workspaceService,
                properties,
                currentAgentService,
                permissionControlService
        );
        ArtifactContentPointer pointer = store.store(
                "artifact-1",
                ArtifactType.REPORT,
                "hello artifact",
                "text/plain",
                "report.txt"
        );

        Path filePath = tempDir.resolve("shared").resolve(pointer.location());
        assertThat(Files.exists(filePath)).isTrue();
        assertThat(Files.readString(filePath)).isEqualTo("hello artifact");
        assertThat(pointer.storageKind()).isEqualTo("workspace");
    }

    @Test
    void rejectsContentFolderOutsideSharedWorkspace() throws Exception {
        WorkspaceService workspaceService = mock(WorkspaceService.class);
        CurrentAgentService currentAgentService = mock(CurrentAgentService.class);
        AgentPermissionControlService permissionControlService = mock(AgentPermissionControlService.class);
        when(workspaceService.getSharedWorkspace()).thenReturn(tempDir.resolve("shared"));
        when(currentAgentService.getCurrentAgent()).thenReturn(new AuthenticatedAgent(UUID.randomUUID(), "agent-1"));
        doNothing().when(permissionControlService).checkSharedWorkspaceWrite("agent-1", "store_artifact:report");

        ArtifactStorageProperties properties = new ArtifactStorageProperties();
        properties.setContentFolder("../outside");

        WorkspaceArtifactContentStore store = new WorkspaceArtifactContentStore(
                workspaceService,
                properties,
                currentAgentService,
                permissionControlService
        );

        assertThatThrownBy(() -> store.store(
                "artifact-1",
                ArtifactType.REPORT,
                "x",
                "text/plain",
                "report.txt"
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("escapes shared workspace");
    }
}
