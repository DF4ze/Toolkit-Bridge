package fr.ses10doigts.toolkitbridge.memory.context.global.service;

import fr.ses10doigts.toolkitbridge.config.workspace.WorkspaceProperties;
import fr.ses10doigts.toolkitbridge.memory.context.global.config.GlobalContextProperties;
import fr.ses10doigts.toolkitbridge.memory.context.global.model.SharedGlobalContextSnapshot;
import fr.ses10doigts.toolkitbridge.service.workspace.WorkspaceLayout;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MarkdownSharedGlobalContextProviderTest {

    @TempDir
    Path tempDir;

    private WorkspaceLayout workspaceLayout;
    private Path globalContextRoot;

    @BeforeEach
    void setUp() throws IOException {
        WorkspaceProperties properties = new WorkspaceProperties();
        properties.setAgentsRoot(tempDir.resolve("agents").toString());
        properties.setSharedRoot(tempDir.resolve("shared").toString());
        properties.setGlobalContextRoot(tempDir.resolve("global-context").toString());

        workspaceLayout = new WorkspaceLayout(properties);
        globalContextRoot = workspaceLayout.globalContextRoot();
    }

    @Test
    void shouldLoadConfiguredMarkdownFiles() throws IOException {
        Files.writeString(globalContextRoot.resolve("profile.md"), "# Profile\nAlice");
        Files.writeString(globalContextRoot.resolve("rules.md"), "# Rules\nBe concise");

        GlobalContextProperties properties = new GlobalContextProperties();
        properties.setFiles(List.of("rules.md", "profile.md"));

        MarkdownSharedGlobalContextProvider provider = new MarkdownSharedGlobalContextProvider(properties, workspaceLayout);

        SharedGlobalContextSnapshot snapshot = provider.getSharedGlobalContext();

        assertThat(snapshot.sourceFiles()).containsExactly("rules.md", "profile.md");
        assertThat(snapshot.content()).contains("### rules.md");
        assertThat(snapshot.content()).contains("### profile.md");
        assertThat(snapshot.content()).contains("# Profile\nAlice");
    }

    @Test
    void shouldReloadImmediatelyInOnDemandMode() throws IOException {
        Path file = globalContextRoot.resolve("profile.md");
        Files.writeString(file, "version-1");

        GlobalContextProperties properties = new GlobalContextProperties();
        properties.setLoadMode(GlobalContextProperties.LoadMode.ON_DEMAND);

        MarkdownSharedGlobalContextProvider provider = new MarkdownSharedGlobalContextProvider(properties, workspaceLayout);

        assertThat(provider.getSharedGlobalContext().content()).contains("version-1");

        Files.writeString(file, "version-2");

        assertThat(provider.getSharedGlobalContext().content()).contains("version-2");
    }

    @Test
    void shouldKeepCachedValueUntilRefreshIntervalExpires() throws IOException {
        Path file = globalContextRoot.resolve("profile.md");
        Files.writeString(file, "cached-v1");

        GlobalContextProperties properties = new GlobalContextProperties();
        properties.setLoadMode(GlobalContextProperties.LoadMode.CACHED);
        properties.setCacheRefreshInterval(Duration.ofHours(1));

        MarkdownSharedGlobalContextProvider provider = new MarkdownSharedGlobalContextProvider(properties, workspaceLayout);

        assertThat(provider.getSharedGlobalContext().content()).contains("cached-v1");

        Files.writeString(file, "cached-v2");

        assertThat(provider.getSharedGlobalContext().content()).contains("cached-v1");
    }

    @Test
    void shouldRefreshCachedValueAfterRefreshInterval() throws Exception {
        Path file = globalContextRoot.resolve("profile.md");
        Files.writeString(file, "cached-v1");

        GlobalContextProperties properties = new GlobalContextProperties();
        properties.setLoadMode(GlobalContextProperties.LoadMode.CACHED);
        properties.setCacheRefreshInterval(Duration.ofMillis(5));

        MarkdownSharedGlobalContextProvider provider = new MarkdownSharedGlobalContextProvider(properties, workspaceLayout);
        assertThat(provider.getSharedGlobalContext().content()).contains("cached-v1");

        Files.writeString(file, "cached-v2");
        Thread.sleep(20L);

        assertThat(provider.getSharedGlobalContext().content()).contains("cached-v2");
    }
}
