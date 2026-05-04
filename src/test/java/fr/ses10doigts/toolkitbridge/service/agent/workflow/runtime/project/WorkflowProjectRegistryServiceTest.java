package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.project;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkflowProjectRegistryServiceTest {

    @Test
    void normalizeProjectNameRejectsInvalidInputs() {
        WorkflowProjectRegistryService service = new WorkflowProjectRegistryService(mock(WorkflowProjectRepository.class), new WorkflowProjectPathPolicy());

        assertThat(service.normalizeProjectName(null).valid()).isFalse();
        assertThat(service.normalizeProjectName("   ").valid()).isFalse();
        assertThat(service.normalizeProjectName("has space").valid()).isFalse();
        assertThat(service.normalizeProjectName("bad@name").valid()).isFalse();
    }

    @Test
    void normalizeProjectNameTrimsAndLowercasesKey() {
        WorkflowProjectRegistryService service = new WorkflowProjectRegistryService(mock(WorkflowProjectRepository.class), new WorkflowProjectPathPolicy());
        WorkflowProjectRegistryService.NormalizedName name = service.normalizeProjectName("  ToolkitBridge  ");
        assertThat(name.valid()).isTrue();
        assertThat(name.projectName()).isEqualTo("ToolkitBridge");
        assertThat(name.projectKey()).isEqualTo("toolkitbridge");
    }

    @Test
    void registerRejectsNonExistingPath(@TempDir Path tempDir) {
        Path missing = tempDir.resolve("missing-dir");
        WorkflowProjectRepository repo = mock(WorkflowProjectRepository.class);
        when(repo.findByProjectKey(anyString())).thenReturn(Optional.empty());

        WorkflowProjectRegistryService service = new WorkflowProjectRegistryService(repo, new WorkflowProjectPathPolicy());
        WorkflowProjectRegistrationResult result = service.registerOrUpdate("ToolkitBridge", missing.toString());

        assertThat(result.success()).isFalse();
        assertThat(result.reason()).contains("does not exist");
    }

    @Test
    void registerRejectsFileInsteadOfDirectory(@TempDir Path tempDir) throws Exception {
        Path file = tempDir.resolve("file.txt");
        Files.writeString(file, "x");

        WorkflowProjectRepository repo = mock(WorkflowProjectRepository.class);
        when(repo.findByProjectKey(anyString())).thenReturn(Optional.empty());

        WorkflowProjectRegistryService service = new WorkflowProjectRegistryService(repo, new WorkflowProjectPathPolicy());
        WorkflowProjectRegistrationResult result = service.registerOrUpdate("ToolkitBridge", file.toString());

        assertThat(result.success()).isFalse();
        assertThat(result.reason()).contains("directory");
    }

    @Test
    void registerCreatesThenUpdates(@TempDir Path tempDir) throws Exception {
        Path dir1 = Files.createDirectory(tempDir.resolve("p1"));
        Path dir2 = Files.createDirectory(tempDir.resolve("p2"));

        WorkflowProjectRepository repo = mock(WorkflowProjectRepository.class);
        WorkflowProjectEntity existing = new WorkflowProjectEntity();
        existing.setId(1L);
        existing.setProjectKey("toolkitbridge");
        existing.setProjectName("ToolkitBridge");
        existing.setProjectPath(dir1.toString());

        when(repo.findByProjectKey("toolkitbridge")).thenReturn(Optional.empty(), Optional.of(existing));
        when(repo.save(any(WorkflowProjectEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        WorkflowProjectRegistryService service = new WorkflowProjectRegistryService(repo, new WorkflowProjectPathPolicy());

        WorkflowProjectRegistrationResult created = service.registerOrUpdate("ToolkitBridge", dir1.toString());
        assertThat(created.success()).isTrue();
        assertThat(created.created()).isTrue();
        assertThat(created.projectKey()).isEqualTo("toolkitbridge");

        WorkflowProjectRegistrationResult updated = service.registerOrUpdate("ToolkitBridge", dir2.toString());
        assertThat(updated.success()).isTrue();
        assertThat(updated.created()).isFalse();
        assertThat(updated.projectKey()).isEqualTo("toolkitbridge");
    }

    @Test
    void lookupUnknownReturnsNotFound() {
        WorkflowProjectRepository repo = mock(WorkflowProjectRepository.class);
        when(repo.findByProjectKey(anyString())).thenReturn(Optional.empty());

        WorkflowProjectRegistryService service = new WorkflowProjectRegistryService(repo, new WorkflowProjectPathPolicy());
        WorkflowProjectLookupResult result = service.lookup("ToolkitBridge");

        assertThat(result.found()).isFalse();
        assertThat(result.reason()).isNotBlank();
    }
}
