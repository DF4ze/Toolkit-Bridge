package fr.ses10doigts.toolkitbridge.service.telegram.workflow;

import fr.ses10doigts.toolkitbridge.config.workspace.WorkspaceProperties;
import fr.ses10doigts.toolkitbridge.service.workspace.WorkspaceLayout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowTelegramRoadmapServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void loadsValidRoadmapAndStoresResolvedPathInSession() throws Exception {
        Path sharedRoot = tempDir.resolve("shared");
        WorkspaceLayout workspaceLayout = workspaceLayout(sharedRoot);
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        WorkflowTelegramRoadmapService service = new WorkflowTelegramRoadmapService(workspaceLayout, store);

        Path roadmap = sharedRoot.resolve("projects/Toolkit/roadmaps/phase7.md");
        Files.createDirectories(roadmap.getParent());
        Files.writeString(roadmap, "# Roadmap\n", StandardCharsets.UTF_8);

        String response = service.loadRoadmap(10L, 20L, "Toolkit", "projects/Toolkit/roadmaps/phase7.md");

        assertThat(response).contains("Action effectuee");
        assertThat(response).contains("Roadmap loaded");
        assertThat(response).contains("projects/Toolkit/roadmaps/phase7.md");

        WorkflowTelegramSession session = store.getOrCreate(10L);
        assertThat(session.roadmapPath()).isEqualTo(roadmap.normalize());
        assertThat(session.projectName()).isEqualTo("Toolkit");
    }

    @Test
    void returnsNotFoundWhenFileDoesNotExist() {
        Path sharedRoot = tempDir.resolve("shared");
        WorkspaceLayout workspaceLayout = workspaceLayout(sharedRoot);
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        WorkflowTelegramRoadmapService service = new WorkflowTelegramRoadmapService(workspaceLayout, store);

        String response = service.loadRoadmap(10L, 20L, "Toolkit", "projects/Toolkit/roadmaps/missing.md");

        assertThat(response).contains(WorkflowTelegramRoadmapService.ROADMAP_NOT_FOUND_MESSAGE);
    }

    @Test
    void rejectsWrongExtension() throws Exception {
        Path sharedRoot = tempDir.resolve("shared");
        WorkspaceLayout workspaceLayout = workspaceLayout(sharedRoot);
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        WorkflowTelegramRoadmapService service = new WorkflowTelegramRoadmapService(workspaceLayout, store);

        Path roadmap = sharedRoot.resolve("projects/Toolkit/roadmaps/phase7.txt");
        Files.createDirectories(roadmap.getParent());
        Files.writeString(roadmap, "x", StandardCharsets.UTF_8);

        String response = service.loadRoadmap(10L, 20L, "Toolkit", "projects/Toolkit/roadmaps/phase7.txt");

        assertThat(response).contains(WorkflowTelegramRoadmapService.ROADMAP_WRONG_EXTENSION_MESSAGE);
    }

    @Test
    void rejectsEmptyFile() throws Exception {
        Path sharedRoot = tempDir.resolve("shared");
        WorkspaceLayout workspaceLayout = workspaceLayout(sharedRoot);
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        WorkflowTelegramRoadmapService service = new WorkflowTelegramRoadmapService(workspaceLayout, store);

        Path roadmap = sharedRoot.resolve("projects/Toolkit/roadmaps/phase7.md");
        Files.createDirectories(roadmap.getParent());
        Files.writeString(roadmap, "", StandardCharsets.UTF_8);

        String response = service.loadRoadmap(10L, 20L, "Toolkit", "projects/Toolkit/roadmaps/phase7.md");

        assertThat(response).contains(WorkflowTelegramRoadmapService.ROADMAP_EMPTY_MESSAGE);
    }

    @Test
    void rejectsWhitespaceOnlyFile() throws Exception {
        Path sharedRoot = tempDir.resolve("shared");
        WorkspaceLayout workspaceLayout = workspaceLayout(sharedRoot);
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        WorkflowTelegramRoadmapService service = new WorkflowTelegramRoadmapService(workspaceLayout, store);

        Path roadmap = sharedRoot.resolve("projects/Toolkit/roadmaps/phase7.md");
        Files.createDirectories(roadmap.getParent());
        Files.writeString(roadmap, "   \n\t\n", StandardCharsets.UTF_8);

        String response = service.loadRoadmap(10L, 20L, "Toolkit", "projects/Toolkit/roadmaps/phase7.md");

        assertThat(response).contains(WorkflowTelegramRoadmapService.ROADMAP_EMPTY_MESSAGE);
    }

    @Test
    void rejectsPathTraversalOutsideSharedRoot() {
        Path sharedRoot = tempDir.resolve("shared");
        WorkspaceLayout workspaceLayout = workspaceLayout(sharedRoot);
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        WorkflowTelegramRoadmapService service = new WorkflowTelegramRoadmapService(workspaceLayout, store);

        String response = service.loadRoadmap(10L, 20L, "Toolkit", "../secrets.md");

        assertThat(response).contains(WorkflowTelegramRoadmapService.INVALID_ROADMAP_PATH_MESSAGE);
    }

    @Test
    void rejectsInvalidRequestWhenChatIdIsMissing() {
        Path sharedRoot = tempDir.resolve("shared");
        WorkspaceLayout workspaceLayout = workspaceLayout(sharedRoot);
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        WorkflowTelegramRoadmapService service = new WorkflowTelegramRoadmapService(workspaceLayout, store);

        String response = service.loadRoadmap(null, 20L, "Toolkit", "projects/Toolkit/roadmaps/phase7.md");

        assertThat(response).contains(WorkflowTelegramRoadmapService.INVALID_REQUEST_MESSAGE);
    }

    private WorkspaceLayout workspaceLayout(Path sharedRoot) {
        WorkspaceProperties properties = new WorkspaceProperties();
        properties.setAgentsRoot(tempDir.resolve("agents").toString());
        properties.setSharedRoot(sharedRoot.toString());
        return new WorkspaceLayout(properties);
    }
}
