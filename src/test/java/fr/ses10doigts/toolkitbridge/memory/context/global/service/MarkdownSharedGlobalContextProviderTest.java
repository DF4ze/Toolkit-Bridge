package fr.ses10doigts.toolkitbridge.memory.context.global.service;

import fr.ses10doigts.toolkitbridge.config.workspace.WorkspaceProperties;
import fr.ses10doigts.toolkitbridge.memory.config.runtime.MemoryRuntimeConfiguration;
import fr.ses10doigts.toolkitbridge.memory.config.runtime.MemoryRuntimeConfigurationResolver;
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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

        MarkdownSharedGlobalContextProvider provider = new MarkdownSharedGlobalContextProvider(
                resolver(true, MemoryRuntimeConfiguration.GlobalContextLoadMode.ON_DEMAND, Duration.ofSeconds(30), List.of("rules.md", "profile.md")),
                workspaceLayout
        );

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

        MarkdownSharedGlobalContextProvider provider = new MarkdownSharedGlobalContextProvider(
                resolver(true, MemoryRuntimeConfiguration.GlobalContextLoadMode.ON_DEMAND, Duration.ofSeconds(30), List.of()),
                workspaceLayout
        );

        assertThat(provider.getSharedGlobalContext().content()).contains("version-1");

        Files.writeString(file, "version-2");

        assertThat(provider.getSharedGlobalContext().content()).contains("version-2");
    }

    @Test
    void shouldKeepCachedValueUntilRefreshIntervalExpires() throws IOException {
        Path file = globalContextRoot.resolve("profile.md");
        Files.writeString(file, "cached-v1");

        MarkdownSharedGlobalContextProvider provider = new MarkdownSharedGlobalContextProvider(
                resolver(true, MemoryRuntimeConfiguration.GlobalContextLoadMode.CACHED, Duration.ofHours(1), List.of()),
                workspaceLayout
        );

        assertThat(provider.getSharedGlobalContext().content()).contains("cached-v1");

        Files.writeString(file, "cached-v2");

        assertThat(provider.getSharedGlobalContext().content()).contains("cached-v1");
    }

    @Test
    void shouldRefreshCachedValueAfterRefreshInterval() throws Exception {
        Path file = globalContextRoot.resolve("profile.md");
        Files.writeString(file, "cached-v1");

        MarkdownSharedGlobalContextProvider provider = new MarkdownSharedGlobalContextProvider(
                resolver(true, MemoryRuntimeConfiguration.GlobalContextLoadMode.CACHED, Duration.ofMillis(5), List.of()),
                workspaceLayout
        );
        assertThat(provider.getSharedGlobalContext().content()).contains("cached-v1");

        Files.writeString(file, "cached-v2");
        Thread.sleep(20L);

        assertThat(provider.getSharedGlobalContext().content()).contains("cached-v2");
    }

    @Test
    void shouldRefreshCachedValueImmediatelyAfterExplicitInvalidation() throws Exception {
        Path file = globalContextRoot.resolve("profile.md");
        Files.writeString(file, "cached-v1");

        MarkdownSharedGlobalContextProvider provider = new MarkdownSharedGlobalContextProvider(
                resolver(true, MemoryRuntimeConfiguration.GlobalContextLoadMode.CACHED, Duration.ofHours(1), List.of()),
                workspaceLayout
        );
        assertThat(provider.getSharedGlobalContext().content()).contains("cached-v1");

        Files.writeString(file, "cached-v2");
        provider.invalidateCache();

        assertThat(provider.getSharedGlobalContext().content()).contains("cached-v2");
    }

    private MemoryRuntimeConfigurationResolver resolver(
            boolean enabled,
            MemoryRuntimeConfiguration.GlobalContextLoadMode loadMode,
            Duration refreshInterval,
            List<String> files
    ) {
        MemoryRuntimeConfigurationResolver resolver = mock(MemoryRuntimeConfigurationResolver.class);
        when(resolver.snapshot()).thenReturn(new MemoryRuntimeConfiguration(
                new MemoryRuntimeConfiguration.Context(10, 10, 15000, 5),
                new MemoryRuntimeConfiguration.Retrieval(10, 10, 25, 5, 5, 4000),
                new MemoryRuntimeConfiguration.Integration(true, true, true, true),
                new MemoryRuntimeConfiguration.Scoring(1.0, 0.5, 1.0),
                new MemoryRuntimeConfiguration.GlobalContext(enabled, loadMode, refreshInterval, files)
        ));
        return resolver;
    }
}
