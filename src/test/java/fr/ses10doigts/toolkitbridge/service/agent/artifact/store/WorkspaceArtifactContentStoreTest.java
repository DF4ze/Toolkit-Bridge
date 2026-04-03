package fr.ses10doigts.toolkitbridge.service.agent.artifact.store;

import fr.ses10doigts.toolkitbridge.service.agent.artifact.config.ArtifactStorageProperties;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactContentPointer;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.ArtifactType;
import fr.ses10doigts.toolkitbridge.service.workspace.WorkspaceService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkspaceArtifactContentStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void storesArtifactPayloadInSharedWorkspace() throws Exception {
        WorkspaceService workspaceService = mock(WorkspaceService.class);
        when(workspaceService.getSharedWorkspace()).thenReturn(tempDir.resolve("shared"));

        ArtifactStorageProperties properties = new ArtifactStorageProperties();
        properties.setContentFolder("artifacts");

        WorkspaceArtifactContentStore store = new WorkspaceArtifactContentStore(workspaceService, properties);
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
        when(workspaceService.getSharedWorkspace()).thenReturn(tempDir.resolve("shared"));

        ArtifactStorageProperties properties = new ArtifactStorageProperties();
        properties.setContentFolder("../outside");

        WorkspaceArtifactContentStore store = new WorkspaceArtifactContentStore(workspaceService, properties);

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
