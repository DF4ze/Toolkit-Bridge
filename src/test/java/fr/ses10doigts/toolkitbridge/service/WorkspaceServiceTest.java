package fr.ses10doigts.toolkitbridge.service;

import fr.ses10doigts.toolkitbridge.exception.ForbiddenCommandException;
import fr.ses10doigts.toolkitbridge.model.dto.auth.AuthenticatedBot;
import fr.ses10doigts.toolkitbridge.service.auth.CurrentBotService;
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

    private CurrentBotService currentBotService;
    private WorkspaceService workspaceService;

    @BeforeEach
    void setUp() throws IOException {
        currentBotService = mock(CurrentBotService.class);
        when(currentBotService.getCurrentBot())
                .thenReturn(new AuthenticatedBot(UUID.randomUUID(), "bot-test"));

        workspaceService = new WorkspaceService(tempDir.toString(), currentBotService);
    }

    @Test
    void shouldCreateCurrentBotWorkspace() throws IOException {
        Path workspace = workspaceService.getCurrentBotWorkspace();

        assertTrue(Files.exists(workspace));
        assertTrue(Files.isDirectory(workspace));
        assertTrue(workspace.startsWith(tempDir));
    }

    @Test
    void shouldResolvePathInsideCurrentBotWorkspace() throws IOException {
        Path resolved = workspaceService.resolveInCurrentBotWorkspace("notes/test.txt");

        assertTrue(resolved.startsWith(tempDir.resolve("bot-test")));
        assertEquals("test.txt", resolved.getFileName().toString());
    }

    @Test
    void shouldRejectAbsolutePath() {
        Path absolutePath = tempDir.toAbsolutePath().resolve("forbidden.txt");

        assertThrows(
                ForbiddenCommandException.class,
                () -> workspaceService.resolveInCurrentBotWorkspace(absolutePath.toString())
        );
    }

    @Test
    void shouldRejectEscapingPath() {
        assertThrows(
                ForbiddenCommandException.class,
                () -> workspaceService.resolveInCurrentBotWorkspace("../secret.txt")
        );
    }

    @Test
    void shouldRelativizePathFromCurrentBotWorkspace() throws IOException {
        Path resolved = workspaceService.resolveInCurrentBotWorkspace("folder/file.txt");

        String relative = workspaceService.relativizeFromCurrentBotWorkspace(resolved);

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