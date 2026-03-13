package fr.ses10doigts.toolkitbridge.service.botservice;

import fr.ses10doigts.toolkitbridge.model.dto.auth.AuthenticatedBot;
import fr.ses10doigts.toolkitbridge.model.dto.web.FileContentResponse;
import fr.ses10doigts.toolkitbridge.model.dto.web.FileEntryResponse;
import fr.ses10doigts.toolkitbridge.model.dto.web.SimpleResponse;
import fr.ses10doigts.toolkitbridge.service.WorkspaceService;
import fr.ses10doigts.toolkitbridge.service.auth.CurrentBotService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BotFileServiceTest {

    @TempDir
    Path tempDir;

    private BotFileService botFileService;
    private WorkspaceService workspaceService;

    @BeforeEach
    void setUp() throws IOException {
        CurrentBotService currentBotService = mock(CurrentBotService.class);
        when(currentBotService.getCurrentBot())
                .thenReturn(new AuthenticatedBot(UUID.randomUUID(), "bot-files"));

        workspaceService = new WorkspaceService(tempDir.toString(), currentBotService);
        botFileService = new BotFileService(workspaceService);
    }

    @Test
    void writeAndReadFileShouldWork() throws IOException {
        SimpleResponse writeResponse = botFileService.writeFile("notes/test.txt", "hello");
        FileContentResponse readResponse = botFileService.readFile("notes/test.txt");

        assertFalse(writeResponse.isError());
        assertEquals("hello", readResponse.content());
        assertEquals("notes/test.txt", readResponse.path());
    }

    @Test
    void appendFileShouldWork() throws IOException {
        botFileService.writeFile("notes/test.txt", "hello");
        botFileService.appendFile("notes/test.txt", " world");

        FileContentResponse readResponse = botFileService.readFile("notes/test.txt");
        assertEquals("hello world", readResponse.content());
    }

    @Test
    void listFilesShouldReturnSortedEntries() throws IOException {
        botFileService.writeFile("notes/zeta.txt", "Z");
        botFileService.writeFile("notes/alpha.txt", "A");

        List<FileEntryResponse> entries = botFileService.listFiles("notes");

        assertEquals(2, entries.size());
        assertEquals("notes/alpha.txt", entries.get(0).path());
        assertEquals("notes/zeta.txt", entries.get(1).path());
    }

    @Test
    void moveFileShouldWork() throws IOException {
        botFileService.writeFile("notes/test.txt", "hello");

        botFileService.moveFile("notes/test.txt", "archive/test.txt");

        assertThrows(IllegalArgumentException.class, () -> botFileService.readFile("notes/test.txt"));
        assertEquals("hello", botFileService.readFile("archive/test.txt").content());
    }

    @Test
    void deleteFileShouldWork() throws IOException {
        botFileService.writeFile("notes/test.txt", "hello");

        botFileService.deleteFile("notes/test.txt");

        assertThrows(IllegalArgumentException.class, () -> botFileService.readFile("notes/test.txt"));
    }

    @Test
    void deleteDirectoryShouldWork() throws IOException {
        botFileService.writeFile("notes/a.txt", "A");
        botFileService.writeFile("notes/sub/b.txt", "B");

        botFileService.deleteFile("notes");

        Path notesDir = workspaceService.resolveInCurrentBotWorkspace("notes");
        assertFalse(Files.exists(notesDir));
    }

    @Test
    void readFileShouldRejectMissingFile() {
        assertThrows(IllegalArgumentException.class, () -> botFileService.readFile("missing.txt"));
    }

    @Test
    void writeFileShouldRejectNullContent() {
        assertThrows(IllegalArgumentException.class, () -> botFileService.writeFile("notes/test.txt", null));
    }
}