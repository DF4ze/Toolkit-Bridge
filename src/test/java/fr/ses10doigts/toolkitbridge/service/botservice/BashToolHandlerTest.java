package fr.ses10doigts.toolkitbridge.service.botservice;

import fr.ses10doigts.toolkitbridge.config.workspace.WorkspaceProperties;
import fr.ses10doigts.toolkitbridge.exception.ForbiddenCommandException;
import fr.ses10doigts.toolkitbridge.model.dto.auth.AuthenticatedAgent;
import fr.ses10doigts.toolkitbridge.model.dto.tool.bash.BashRequest;
import fr.ses10doigts.toolkitbridge.service.workspace.WorkspaceLayout;
import fr.ses10doigts.toolkitbridge.service.workspace.WorkspaceService;
import fr.ses10doigts.toolkitbridge.service.auth.CurrentAgentService;
import fr.ses10doigts.toolkitbridge.service.tool.bash.BashSecurityService;
import fr.ses10doigts.toolkitbridge.service.tool.bash.BashToolHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BashToolHandlerTest {

    @TempDir
    Path tempDir;

    private BashToolHandler bashToolHandler;

    @BeforeEach
    void setUp() throws IOException {
        CurrentAgentService currentAgentService = mock(CurrentAgentService.class);
        when(currentAgentService.getCurrentAgent())
                .thenReturn(new AuthenticatedAgent(UUID.randomUUID(), "bot-bash"));

        WorkspaceProperties properties = new WorkspaceProperties();
        properties.setAgentsRoot(tempDir.resolve("agents").toString());
        properties.setSharedRoot(tempDir.resolve("shared").toString());
        properties.setGlobalContextRoot(tempDir.resolve("global-context").toString());

        WorkspaceService workspaceService = new WorkspaceService(new WorkspaceLayout(properties), currentAgentService);
        BashSecurityService securityService = new BashSecurityService(workspaceService);
        bashToolHandler = new BashToolHandler(workspaceService, securityService);
    }

    @Test
    void shouldRejectUnknownTool() {
        BashRequest request = new BashRequest();
        request.setTool("rm");
        request.setArgs(List.of());
        request.setTimeout(5);

        assertThrows(ForbiddenCommandException.class, () -> bashToolHandler.execute(request));
    }

    @Test
    void shouldRejectPwdWithArguments() {
        BashRequest request = new BashRequest();
        request.setTool("pwd");
        request.setArgs(List.of("oops"));
        request.setTimeout(5);

        assertThrows(IllegalArgumentException.class, () -> bashToolHandler.execute(request));
    }

    @Test
    void shouldRejectDateWithArguments() {
        BashRequest request = new BashRequest();
        request.setTool("date");
        request.setArgs(List.of("+%Y-%m-%d"));
        request.setTimeout(5);

        assertThrows(IllegalArgumentException.class, () -> bashToolHandler.execute(request));
    }

    @Test
    void shouldRejectCatWithoutFile() {
        BashRequest request = new BashRequest();
        request.setTool("cat");
        request.setArgs(List.of());
        request.setTimeout(5);

        assertThrows(IllegalArgumentException.class, () -> bashToolHandler.execute(request));
    }

    @Test
    void shouldRejectCatOutsideWorkspace() {
        BashRequest request = new BashRequest();
        request.setTool("cat");
        request.setArgs(List.of("../secret.txt"));
        request.setTimeout(5);

        assertThrows(ForbiddenCommandException.class, () -> bashToolHandler.execute(request));
    }

    @Test
    void shouldRejectLsOutsideWorkspace() {
        BashRequest request = new BashRequest();
        request.setTool("ls");
        request.setArgs(List.of("../other-bot"));
        request.setTimeout(5);

        assertThrows(ForbiddenCommandException.class, () -> bashToolHandler.execute(request));
    }

    @Test
    void shouldRejectFindOutsideWorkspace() {
        BashRequest request = new BashRequest();
        request.setTool("find");
        request.setArgs(List.of("../other-bot", "-name", "*.txt"));
        request.setTimeout(5);

        assertThrows(ForbiddenCommandException.class, () -> bashToolHandler.execute(request));
    }

    @Test
    void shouldRejectGrepWithoutPattern() {
        BashRequest request = new BashRequest();
        request.setTool("grep");
        request.setArgs(List.of());
        request.setTimeout(5);

        assertThrows(IllegalArgumentException.class, () -> bashToolHandler.execute(request));
    }
}
