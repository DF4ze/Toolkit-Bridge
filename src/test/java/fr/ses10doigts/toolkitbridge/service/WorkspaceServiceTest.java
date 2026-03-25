package fr.ses10doigts.toolkitbridge.service;

import fr.ses10doigts.toolkitbridge.config.workspace.WorkspaceProperties;
import fr.ses10doigts.toolkitbridge.exception.ForbiddenCommandException;
import fr.ses10doigts.toolkitbridge.model.dto.auth.AuthenticatedAgent;
import fr.ses10doigts.toolkitbridge.service.auth.CurrentAgentService;
import fr.ses10doigts.toolkitbridge.service.workspace.WorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WorkspaceServiceTest {

    @TempDir
    Path tempDir;

    private CurrentAgentService currentAgentService;
    private WorkspaceService workspaceService;

    @BeforeEach
    void setUp() throws IOException {
        currentAgentService = mock(CurrentAgentService.class);
        when(currentAgentService.getCurrentAgent())
                .thenReturn(new AuthenticatedAgent(UUID.randomUUID(), "bot-test"));

        WorkspaceProperties properties = new WorkspaceProperties();
        properties.setAgentsRoot(tempDir.resolve("agents").toString());
        properties.setSharedRoot(tempDir.resolve("shared").toString());

        workspaceService = new WorkspaceService(properties, currentAgentService);
    }

    @Test
    void shouldCreateCurrentBotWorkspace() throws IOException {
        Path workspace = workspaceService.getCurrentAgentWorkspace();

        assertTrue(Files.exists(workspace));
        assertTrue(Files.isDirectory(workspace));
        assertTrue(workspace.startsWith(tempDir.resolve("agents")));
    }

    @Test
    void shouldResolvePathInsideCurrentBotWorkspace() throws IOException {
        Path resolved = workspaceService.resolveInCurrentAgentWorkspace("notes/test.txt");

        assertTrue(resolved.startsWith(tempDir.resolve("agents").resolve("bot-test")));
        assertEquals("test.txt", resolved.getFileName().toString());
    }

    @Test
    void shouldRejectAbsolutePath() {
        Path absolutePath = tempDir.toAbsolutePath().resolve("forbidden.txt");

        assertThrows(
                ForbiddenCommandException.class,
                () -> workspaceService.resolveInCurrentAgentWorkspace(absolutePath.toString())
        );
    }

    @Test
    void shouldRejectEscapingPath() {
        assertThrows(
                ForbiddenCommandException.class,
                () -> workspaceService.resolveInCurrentAgentWorkspace("../secret.txt")
        );
    }

    @Test
    void shouldRelativizePathFromCurrentBotWorkspace() throws IOException {
        Path resolved = workspaceService.resolveInCurrentAgentWorkspace("folder/file.txt");

        String relative = workspaceService.relativizeFromCurrentAgentWorkspace(resolved);

        assertEquals("folder/file.txt", relative);
    }

    @Test
    void shouldValidateSimpleCommandArg() {
        assertDoesNotThrow(() -> workspaceService.validateCommandArg("hello.txt"));
        assertDoesNotThrow(() -> workspaceService.validateCommandArg("-l"));
        assertDoesNotThrow(() -> workspaceService.validateCommandArg("abc_123"));
    }

    @Test
    void shouldRejectInvalidCommandArg() {
        assertThrows(IllegalArgumentException.class,
                () -> workspaceService.validateCommandArg("bad\narg"));
    }

    @Test
    void shouldRejectNullCommandArg() {
        assertThrows(IllegalArgumentException.class,
                () -> workspaceService.validateCommandArg(null));
    }

    @Test
    void shouldValidateRelativeWorkspacePathArg() {
        assertDoesNotThrow(() -> workspaceService.validateRelativeWorkspacePathArg("folder/file.txt"));
    }

    @Test
    void shouldRejectRelativeWorkspacePathArgOutsideWorkspace() {
        assertThrows(
                ForbiddenCommandException.class,
                () -> workspaceService.validateRelativeWorkspacePathArg("../file.txt")
        );
    }
}
