package fr.ses10doigts.toolkitbridge.service.agent.process.store;

import fr.ses10doigts.toolkitbridge.config.workspace.WorkspaceProperties;
import fr.ses10doigts.toolkitbridge.service.agent.process.model.ExternalProcessHistoryEntry;
import fr.ses10doigts.toolkitbridge.service.agent.process.model.ExternalProcessSnapshot;
import fr.ses10doigts.toolkitbridge.service.agent.process.model.ExternalProcessUpdateRequest;
import fr.ses10doigts.toolkitbridge.service.auth.CurrentAgentService;
import fr.ses10doigts.toolkitbridge.service.workspace.WorkspaceLayout;
import fr.ses10doigts.toolkitbridge.service.workspace.WorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkspaceExternalProcessStoreTest {

    @TempDir
    Path tempDir;

    private WorkspaceExternalProcessStore store;

    @BeforeEach
    void setUp() throws IOException {
        WorkspaceProperties properties = new WorkspaceProperties();
        properties.setAgentsRoot(tempDir.resolve("agents").toString());
        properties.setSharedRoot(tempDir.resolve("shared").toString());
        properties.setGlobalContextRoot(tempDir.resolve("global-context").toString());
        properties.setScriptedToolsRoot(tempDir.resolve("shared").resolve("scripted-tools").toString());
        properties.setExternalProcessesRoot(tempDir.resolve("shared").resolve("processes").toString());

        WorkspaceLayout workspaceLayout = new WorkspaceLayout(properties);
        WorkspaceService workspaceService = new WorkspaceService(workspaceLayout, org.mockito.Mockito.mock(CurrentAgentService.class));
        store = new WorkspaceExternalProcessStore(
                workspaceService,
                workspaceLayout,
                new ObjectMapper()
        );
    }

    @Test
    void savesProcessAndKeepsSimpleHistory() {
        ExternalProcessSnapshot created = store.save(new ExternalProcessUpdateRequest(
                "task-execution-prompt",
                "Task prompt contract",
                "{\"template\":\"v1\"}",
                "application/json",
                "agent-1",
                "create base process"
        ));

        assertThat(created.processId()).isEqualTo("task-execution-prompt");
        assertThat(created.content()).contains("\"v1\"");

        List<ExternalProcessHistoryEntry> history = store.loadHistory("task-execution-prompt");
        assertThat(history).hasSize(1);
        assertThat(history.getFirst().backupPath()).isNull();
    }

    @Test
    void createsBackupBeforeOverwritingExistingProcess() throws IOException {
        store.save(new ExternalProcessUpdateRequest(
                "task-execution-prompt",
                "Task prompt contract",
                "{\"template\":\"v1\"}",
                "application/json",
                "agent-1",
                "create base process"
        ));

        ExternalProcessSnapshot updated = store.save(new ExternalProcessUpdateRequest(
                "task-execution-prompt",
                "Task prompt contract",
                "{\"template\":\"v2\"}",
                "application/json",
                "agent-1",
                "refine task prompt"
        ));

        assertThat(updated.content()).contains("\"v2\"");

        List<ExternalProcessHistoryEntry> history = store.loadHistory("task-execution-prompt");
        assertThat(history).hasSize(2);
        assertThat(history.get(1).backupPath()).isNotBlank();

        Path backupsRoot = tempDir.resolve("shared").resolve("processes").resolve(history.get(1).backupPath());
        assertThat(Files.exists(backupsRoot.resolve("content.json"))).isTrue();
        assertThat(Files.readString(backupsRoot.resolve("content.json"))).contains("\"v1\"");
    }
}
