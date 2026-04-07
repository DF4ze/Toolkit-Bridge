package fr.ses10doigts.toolkitbridge.service.tool.scripted.store;

import fr.ses10doigts.toolkitbridge.config.workspace.WorkspaceProperties;
import fr.ses10doigts.toolkitbridge.service.workspace.WorkspaceLayout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class WorkspaceScriptedToolContentStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void savesAndLoadsToolContentUnderDedicatedPersistentRoot() {
        WorkspaceScriptedToolContentStore store = new WorkspaceScriptedToolContentStore(
                new WorkspaceLayout(properties())
        );

        String relativePath = store.save("deploy-report", 3, "sh", "echo hello");

        assertThat(relativePath).isEqualTo("deploy-report/v3/tool.sh");
        assertThat(store.load(relativePath)).isEqualTo("echo hello");
        assertThat(tempDir.resolve("shared").resolve("scripted-tools").resolve(relativePath)).exists();
    }

    @Test
    void deletesPersistedToolContent() {
        WorkspaceScriptedToolContentStore store = new WorkspaceScriptedToolContentStore(
                new WorkspaceLayout(properties())
        );

        String relativePath = store.save("deploy-report", 1, "sh", "echo hello");
        store.delete(relativePath);

        assertThat(tempDir.resolve("shared").resolve("scripted-tools").resolve(relativePath)).doesNotExist();
    }

    private WorkspaceProperties properties() {
        WorkspaceProperties properties = new WorkspaceProperties();
        properties.setAgentsRoot(tempDir.resolve("agents").toString());
        properties.setSharedRoot(tempDir.resolve("shared").toString());
        properties.setGlobalContextRoot(tempDir.resolve("global-context").toString());
        properties.setScriptedToolsRoot(tempDir.resolve("shared").resolve("scripted-tools").toString());
        properties.setExternalProcessesRoot(tempDir.resolve("shared").resolve("processes").toString());
        return properties;
    }
}
