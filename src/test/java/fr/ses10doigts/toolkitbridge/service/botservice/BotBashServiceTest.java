package fr.ses10doigts.toolkitbridge.service.botservice;

import fr.ses10doigts.toolkitbridge.exception.ForbiddenCommandException;
import fr.ses10doigts.toolkitbridge.model.dto.auth.AuthenticatedBot;
import fr.ses10doigts.toolkitbridge.model.dto.web.CommandRequest;
import fr.ses10doigts.toolkitbridge.service.WorkspaceService;
import fr.ses10doigts.toolkitbridge.service.auth.CurrentBotService;
import fr.ses10doigts.toolkitbridge.service.botservice.BotBashService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BotBashServiceTest {

    @TempDir
    Path tempDir;

    private BotBashService botBashService;

    @BeforeEach
    void setUp() throws IOException {
        CurrentBotService currentBotService = mock(CurrentBotService.class);
        when(currentBotService.getCurrentBot())
                .thenReturn(new AuthenticatedBot(UUID.randomUUID(), "bot-bash"));

        WorkspaceService workspaceService = new WorkspaceService(tempDir.toString(), currentBotService);
        botBashService = new BotBashService(workspaceService);
    }

    @Test
    void shouldRejectUnknownTool() {
        CommandRequest request = new CommandRequest();
        request.setTool("rm");
        request.setArgs(List.of());
        request.setTimeout(5);

        assertThrows(ForbiddenCommandException.class, () -> botBashService.execute(request));
    }

    @Test
    void shouldRejectPwdWithArguments() {
        CommandRequest request = new CommandRequest();
        request.setTool("pwd");
        request.setArgs(List.of("oops"));
        request.setTimeout(5);

        assertThrows(IllegalArgumentException.class, () -> botBashService.execute(request));
    }

    @Test
    void shouldRejectDateWithArguments() {
        CommandRequest request = new CommandRequest();
        request.setTool("date");
        request.setArgs(List.of("+%Y-%m-%d"));
        request.setTimeout(5);

        assertThrows(IllegalArgumentException.class, () -> botBashService.execute(request));
    }

    @Test
    void shouldRejectCatWithoutFile() {
        CommandRequest request = new CommandRequest();
        request.setTool("cat");
        request.setArgs(List.of());
        request.setTimeout(5);

        assertThrows(IllegalArgumentException.class, () -> botBashService.execute(request));
    }

    @Test
    void shouldRejectCatOutsideWorkspace() {
        CommandRequest request = new CommandRequest();
        request.setTool("cat");
        request.setArgs(List.of("../secret.txt"));
        request.setTimeout(5);

        assertThrows(ForbiddenCommandException.class, () -> botBashService.execute(request));
    }

    @Test
    void shouldRejectLsOutsideWorkspace() {
        CommandRequest request = new CommandRequest();
        request.setTool("ls");
        request.setArgs(List.of("../other-bot"));
        request.setTimeout(5);

        assertThrows(ForbiddenCommandException.class, () -> botBashService.execute(request));
    }

    @Test
    void shouldRejectFindOutsideWorkspace() {
        CommandRequest request = new CommandRequest();
        request.setTool("find");
        request.setArgs(List.of("../other-bot", "-name", "*.txt"));
        request.setTimeout(5);

        assertThrows(ForbiddenCommandException.class, () -> botBashService.execute(request));
    }

    @Test
    void shouldRejectGrepWithoutPattern() {
        CommandRequest request = new CommandRequest();
        request.setTool("grep");
        request.setArgs(List.of());
        request.setTimeout(5);

        assertThrows(IllegalArgumentException.class, () -> botBashService.execute(request));
    }
}