package fr.ses10doigts.toolkitbridge.service.botservice;

import fr.ses10doigts.toolkitbridge.exception.ToolExecutionException;
import fr.ses10doigts.toolkitbridge.exception.ToolValidationException;
import fr.ses10doigts.toolkitbridge.config.workspace.WorkspaceProperties;
import fr.ses10doigts.toolkitbridge.model.dto.auth.AuthenticatedAgent;
import fr.ses10doigts.toolkitbridge.model.dto.tool.ToolExecutionResult;
import fr.ses10doigts.toolkitbridge.service.workspace.WorkspaceService;
import fr.ses10doigts.toolkitbridge.service.workspace.WorkspaceLayout;
import fr.ses10doigts.toolkitbridge.service.auth.CurrentAgentService;
import fr.ses10doigts.toolkitbridge.service.tool.file.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FileToolHandlerTest {

    @TempDir
    Path tempDir;

    private WorkspaceService workspaceService;

    private WriteFileToolHandler writeFileToolHandler;
    private ReadFileToolHandler readFileToolHandler;
    private AppendFileToolHandler appendFileToolHandler;
    private ListFilesToolHandler listFilesToolHandler;
    private MoveFileToolHandler moveFileToolHandler;
    private DeleteFileToolHandler deleteFileToolHandler;

    @BeforeEach
    void setUp() throws IOException {
        CurrentAgentService currentAgentService = mock(CurrentAgentService.class);
        when(currentAgentService.getCurrentAgent())
                .thenReturn(new AuthenticatedAgent(UUID.randomUUID(), "bot-files"));

        WorkspaceProperties properties = new WorkspaceProperties();
        properties.setAgentsRoot(tempDir.resolve("agents").toString());
        properties.setSharedRoot(tempDir.resolve("shared").toString());
        properties.setGlobalContextRoot(tempDir.resolve("global-context").toString());

        workspaceService = new WorkspaceService(new WorkspaceLayout(properties), currentAgentService);

        writeFileToolHandler = new WriteFileToolHandler(workspaceService);
        readFileToolHandler = new ReadFileToolHandler(workspaceService);
        appendFileToolHandler = new AppendFileToolHandler(workspaceService);
        listFilesToolHandler = new ListFilesToolHandler(workspaceService);
        moveFileToolHandler = new MoveFileToolHandler(workspaceService);
        deleteFileToolHandler = new DeleteFileToolHandler(workspaceService);
    }

    @Test
    void writeAndReadFileShouldWork() throws IOException {
        ToolExecutionResult writeResponse = writeFileToolHandler.execute(Map.of(
                "path", "notes/test.txt",
                "content", "hello"
        ));

        ToolExecutionResult readResponse = readFileToolHandler.execute(Map.of(
                "path", "notes/test.txt"
        ));

        assertFalse(writeResponse.isError());
        assertEquals("hello", readResponse.getFile().getContent().content());
        assertEquals("notes/test.txt", readResponse.getFile().getPath());
    }

    @Test
    void appendFileShouldWork() throws IOException {
        writeFileToolHandler.execute(Map.of(
                "path", "notes/test.txt",
                "content", "hello"
        ));

        appendFileToolHandler.execute(Map.of(
                "path", "notes/test.txt",
                "content", " world"
        ));

        ToolExecutionResult readResponse = readFileToolHandler.execute(Map.of(
                "path", "notes/test.txt"
        ));

        assertEquals("hello world", readResponse.getFile().getContent().content());
    }

    @Test
    void listFilesShouldReturnSortedEntries() throws IOException {
        writeFileToolHandler.execute(Map.of(
                "path", "notes/zeta.txt",
                "content", "Z"
        ));
        writeFileToolHandler.execute(Map.of(
                "path", "notes/alpha.txt",
                "content", "A"
        ));

        ToolExecutionResult entries = listFilesToolHandler.execute(Map.of(
                "path", "notes"
        ));

        assertEquals(2, entries.getFile().getEntry().size());
        assertEquals("notes/alpha.txt", entries.getFile().getEntry().get(0).path());
        assertEquals("notes/zeta.txt", entries.getFile().getEntry().get(1).path());
    }

    @Test
    void moveFileShouldWork() throws IOException {
        writeFileToolHandler.execute(Map.of(
                "path", "notes/test.txt",
                "content", "hello"
        ));

        moveFileToolHandler.execute(Map.of(
                "source_path", "notes/test.txt",
                "target_path", "archive/test.txt"
        ));

        ToolExecutionResult result = readFileToolHandler.execute(Map.of(
                "path", "archive/test.txt"
        ));

        assertThrows(ToolExecutionException.class, () ->
                readFileToolHandler.execute(Map.of("path", "notes/test.txt"))
        );
        assertEquals("hello", result.getFile().getContent().content());
    }

    @Test
    void deleteFileShouldWork() throws IOException {
        writeFileToolHandler.execute(Map.of(
                "path", "notes/test.txt",
                "content", "hello"
        ));

        deleteFileToolHandler.execute(Map.of(
                "path", "notes/test.txt"
        ));

        assertThrows(ToolExecutionException.class, () ->
                readFileToolHandler.execute(Map.of("path", "notes/test.txt"))
        );
    }

    @Test
    void deleteDirectoryShouldWork() throws IOException {
        writeFileToolHandler.execute(Map.of(
                "path", "notes/a.txt",
                "content", "A"
        ));
        writeFileToolHandler.execute(Map.of(
                "path", "notes/sub/b.txt",
                "content", "B"
        ));

        deleteFileToolHandler.execute(Map.of(
                "path", "notes"
        ));

        Path notesDir = workspaceService.resolveInCurrentAgentWorkspace("notes");
        assertFalse(Files.exists(notesDir));
    }

    @Test
    void readFileShouldRejectMissingFile() {
        assertThrows(ToolExecutionException.class, () ->
                readFileToolHandler.execute(Map.of("path", "missing.txt"))
        );
    }

    @Test
    void writeFileShouldRejectNullContent() {
        Map<String, Object> arguments = new java.util.HashMap<>();
        arguments.put("path", "notes/test.txt");
        arguments.put("content", null);

        assertThrows(ToolValidationException.class, () ->
                writeFileToolHandler.execute(arguments)
        );
    }
}
