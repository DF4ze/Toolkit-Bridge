package fr.ses10doigts.toolkitbridge.service.telegram.workflow;

import fr.ses10doigts.telegrambots.model.TelegramUpdateContext;
import fr.ses10doigts.telegrambots.service.sender.TelegramSender;
import fr.ses10doigts.telegrambots.service.sender.TelegramSenderRegistry;
import fr.ses10doigts.toolkitbridge.config.telegram.workflow.WorkflowTelegramProperties;
import fr.ses10doigts.toolkitbridge.config.workspace.WorkspaceProperties;
import fr.ses10doigts.toolkitbridge.service.workspace.WorkspaceLayout;
import fr.ses10doigts.toolkitbridge.service.workspace.WorkspaceTextFileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class WorkflowTelegramSummaryServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void returnsNoSummaryMessageWhenSessionHasNoPath() {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();

        WorkflowTelegramSummaryService service = newService(store, Optional.empty(), false, tempDir.resolve("data/rapport"));

        TelegramUpdateContext ctx = mock(TelegramUpdateContext.class);
        when(ctx.getChatId()).thenReturn(1L);

        String response = service.workflowSummary(ctx);

        assertThat(response).contains("No summary available");
    }

    @Test
    void returnsNotFoundWhenFileDoesNotExist() {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        Path reportRoot = tempDir.resolve("data/rapport").toAbsolutePath().normalize();
        Path summaryPath = reportRoot.resolve("v1.1/CodexTime/Phase7/etape2/workflow-summary.md").normalize();
        store.tryMarkRunning(1L, "run-1");
        store.completeRun(1L, "run-1", WorkflowTelegramRunStatus.COMPLETED, summaryPath, "done");

        WorkflowTelegramSummaryService service = newService(store, Optional.empty(), false, reportRoot);

        TelegramUpdateContext ctx = mock(TelegramUpdateContext.class);
        when(ctx.getChatId()).thenReturn(1L);

        String response = service.workflowSummary(ctx);

        assertThat(response).contains("Summary not found");
    }

    @Test
    void returnsEmptyWhenFileIsBlank() throws Exception {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        Path reportRoot = tempDir.resolve("data/rapport").toAbsolutePath().normalize();
        Path summaryPath = reportRoot.resolve("v1.1/CodexTime/Phase7/etape2/workflow-summary.md").normalize();
        Files.createDirectories(summaryPath.getParent());
        Files.writeString(summaryPath, "   \n  ", StandardCharsets.UTF_8);
        store.tryMarkRunning(1L, "run-1");
        store.completeRun(1L, "run-1", WorkflowTelegramRunStatus.COMPLETED, summaryPath, "done");

        WorkflowTelegramSummaryService service = newService(store, Optional.empty(), false, reportRoot);

        TelegramUpdateContext ctx = mock(TelegramUpdateContext.class);
        when(ctx.getChatId()).thenReturn(1L);

        String response = service.workflowSummary(ctx);

        assertThat(response).contains("Summary is empty");
    }

    @Test
    void returnsSingleMessageWhenContentIsShort() throws Exception {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        Path reportRoot = tempDir.resolve("data/rapport").toAbsolutePath().normalize();
        Path summaryPath = reportRoot.resolve("v1.1/CodexTime/Phase7/etape2/workflow-summary.md").normalize();
        Files.createDirectories(summaryPath.getParent());
        Files.writeString(summaryPath, "Status: OK\nReason: done\n", StandardCharsets.UTF_8);
        store.tryMarkRunning(1L, "run-1");
        store.completeRun(1L, "run-1", WorkflowTelegramRunStatus.COMPLETED, summaryPath, "done");

        WorkflowTelegramSummaryService service = newService(store, Optional.empty(), false, reportRoot);

        TelegramUpdateContext ctx = mock(TelegramUpdateContext.class);
        when(ctx.getChatId()).thenReturn(1L);

        String response = service.workflowSummary(ctx);

        assertThat(response).startsWith("Workflow summary (1/1)\n");
        assertThat(response).contains("Status: OK");
        assertThat(response.length()).isLessThanOrEqualTo(WorkflowTelegramSummaryService.MAX_TELEGRAM_TEXT);
    }

    @Test
    void splitsInTwoMessagesWhenContentIsMedium() throws Exception {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        Path reportRoot = tempDir.resolve("data/rapport").toAbsolutePath().normalize();
        Path summaryPath = reportRoot.resolve("v1.1/CodexTime/Phase7/etape2/workflow-summary.md").normalize();
        Files.createDirectories(summaryPath.getParent());

        StringBuilder sb = new StringBuilder();
        sb.append("Header\n");
        while (sb.length() < WorkflowTelegramSummaryService.MAX_TELEGRAM_TEXT + 200) {
            sb.append("line ").append(sb.length()).append('\n');
        }
        Files.writeString(summaryPath, sb.toString(), StandardCharsets.UTF_8);
        store.tryMarkRunning(1L, "run-1");
        store.completeRun(1L, "run-1", WorkflowTelegramRunStatus.COMPLETED, summaryPath, "done");

        TelegramSender sender = mock(TelegramSender.class);
        TelegramSenderRegistry registry = mock(TelegramSenderRegistry.class);
        when(registry.getRequiredSender("Cortex")).thenReturn(sender);

        WorkflowTelegramSummaryService service = newService(store, Optional.of(registry), true, reportRoot);

        TelegramUpdateContext ctx = mock(TelegramUpdateContext.class);
        when(ctx.getChatId()).thenReturn(1L);
        when(ctx.getBotId()).thenReturn("Cortex");

        String response = service.workflowSummary(ctx);

        assertThat(response).startsWith("Workflow summary (1/2)\n");
        verify(sender).sendMessage(eq(1L), startsWith("Workflow summary (2/2)\n"));
        assertThat(response.length()).isLessThanOrEqualTo(WorkflowTelegramSummaryService.MAX_TELEGRAM_TEXT);
    }

    @Test
    void sendsDocumentWhenContentIsTooLong() throws Exception {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        Path reportRoot = tempDir.resolve("data/rapport").toAbsolutePath().normalize();
        Path summaryPath = reportRoot.resolve("v1.1/CodexTime/Phase7/etape2/workflow-summary.md").normalize();
        Files.createDirectories(summaryPath.getParent());

        StringBuilder sb = new StringBuilder();
        sb.append("Header\n");
        while (sb.length() < (WorkflowTelegramSummaryService.MAX_TELEGRAM_TEXT * 3)) {
            sb.append("line ").append(sb.length()).append('\n');
        }
        Files.writeString(summaryPath, sb.toString(), StandardCharsets.UTF_8);
        store.tryMarkRunning(1L, "run-1");
        store.completeRun(1L, "run-1", WorkflowTelegramRunStatus.COMPLETED, summaryPath, "done");

        TelegramSender sender = mock(TelegramSender.class);
        TelegramSenderRegistry registry = mock(TelegramSenderRegistry.class);
        when(registry.getRequiredSender("Cortex")).thenReturn(sender);

        WorkflowTelegramSummaryService service = newService(store, Optional.of(registry), true, reportRoot);

        TelegramUpdateContext ctx = mock(TelegramUpdateContext.class);
        when(ctx.getChatId()).thenReturn(1L);
        when(ctx.getBotId()).thenReturn("Cortex");

        String response = service.workflowSummary(ctx);

        assertThat(response).contains("Action effectuee");
        assertThat(response).contains("Summary too long");
        assertThat(response).contains("Complete file sent");
        assertThat(response).contains("Open the attached document");
        verify(sender).sendDocument(eq(1L), eq(summaryPath.toString()), anyString());
    }

    @Test
    void doesNotClaimToSendDocumentWhenContentIsTooLongButNoSenderIsAvailable() throws Exception {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        Path reportRoot = tempDir.resolve("data/rapport").toAbsolutePath().normalize();
        Path summaryPath = reportRoot.resolve("v1.1/CodexTime/Phase7/etape2/workflow-summary.md").normalize();
        Files.createDirectories(summaryPath.getParent());

        StringBuilder sb = new StringBuilder();
        sb.append("Header\n");
        while (sb.length() < (WorkflowTelegramSummaryService.MAX_TELEGRAM_TEXT * 3)) {
            sb.append("line ").append(sb.length()).append('\n');
        }
        Files.writeString(summaryPath, sb.toString(), StandardCharsets.UTF_8);
        store.tryMarkRunning(1L, "run-1");
        store.completeRun(1L, "run-1", WorkflowTelegramRunStatus.COMPLETED, summaryPath, "done");

        WorkflowTelegramSummaryService service = newService(store, Optional.empty(), false, reportRoot);

        TelegramUpdateContext ctx = mock(TelegramUpdateContext.class);
        when(ctx.getChatId()).thenReturn(1L);

        String response = service.workflowSummary(ctx);

        assertThat(response).contains("Action impossible");
        assertThat(response).contains("Summary too long");
        assertThat(response).contains("could not be sent");
    }

    @Test
    void returnsErrorWhenContentIsTooLongButSendDocumentThrows() throws Exception {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        Path reportRoot = tempDir.resolve("data/rapport").toAbsolutePath().normalize();
        Path summaryPath = reportRoot.resolve("v1.1/CodexTime/Phase7/etape2/workflow-summary.md").normalize();
        Files.createDirectories(summaryPath.getParent());

        StringBuilder sb = new StringBuilder();
        sb.append("Header\n");
        while (sb.length() < (WorkflowTelegramSummaryService.MAX_TELEGRAM_TEXT * 3)) {
            sb.append("line ").append(sb.length()).append('\n');
        }
        Files.writeString(summaryPath, sb.toString(), StandardCharsets.UTF_8);
        store.tryMarkRunning(1L, "run-1");
        store.completeRun(1L, "run-1", WorkflowTelegramRunStatus.COMPLETED, summaryPath, "done");

        TelegramSender sender = mock(TelegramSender.class);
        doThrow(new RuntimeException("send failed")).when(sender).sendDocument(anyLong(), anyString(), anyString());

        TelegramSenderRegistry registry = mock(TelegramSenderRegistry.class);
        when(registry.getRequiredSender("Cortex")).thenReturn(sender);

        WorkflowTelegramSummaryService service = newService(store, Optional.of(registry), true, reportRoot);

        TelegramUpdateContext ctx = mock(TelegramUpdateContext.class);
        when(ctx.getChatId()).thenReturn(1L);
        when(ctx.getBotId()).thenReturn("Cortex");

        String response = service.workflowSummary(ctx);

        assertThat(response).contains("Action impossible");
        assertThat(response).contains("Summary too long");
        assertThat(response).contains("could not be sent");
    }

    private WorkflowTelegramSummaryService newService(WorkflowTelegramSessionStore store,
                                                     Optional<TelegramSenderRegistry> registry,
                                                     boolean telegramEnabled,
                                                     Path reportRootDirectory) {
        WorkspaceProperties workspaceProperties = new WorkspaceProperties();
        workspaceProperties.setAgentsRoot(tempDir.resolve("workspace/agents").toString());
        workspaceProperties.setSharedRoot(tempDir.resolve("workspace/shared").toString());

        WorkspaceLayout workspaceLayout = new WorkspaceLayout(workspaceProperties);
        WorkspaceTextFileService textFileService = new WorkspaceTextFileService();

        WorkflowTelegramProperties workflowTelegramProperties = new WorkflowTelegramProperties();
        workflowTelegramProperties.setReportRootDirectory(reportRootDirectory.toString());
        workflowTelegramProperties.setReportVersion("v1.1");

        return new WorkflowTelegramSummaryService(
                store,
                workspaceLayout,
                textFileService,
                registry,
                telegramEnabled,
                workflowTelegramProperties
        );
    }
}
