package fr.ses10doigts.toolkitbridge.service.telegram.workflow;

import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.AnalysisReviewWorkflowRunner;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.project.WorkflowProjectLookupResult;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.project.WorkflowProjectRegistryService;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.WorkflowStepDecision;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.WorkflowStepResult;
import fr.ses10doigts.toolkitbridge.config.workspace.WorkspaceProperties;
import fr.ses10doigts.toolkitbridge.service.workspace.WorkspaceLayout;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WorkflowTelegramOrchestrationServiceTest {

    private static final String WINDOWS_ABSOLUTE_PATH_MARKER = ":\\";
    private static final String UNIX_ABSOLUTE_PATH_MARKER = "/home/";

    @TempDir
    Path tempDir;

    private WorkflowProjectRegistryService registryFound() {
        WorkflowProjectRegistryService registry = mock(WorkflowProjectRegistryService.class);
        when(registry.lookup(anyString())).thenAnswer(inv -> {
            String name = inv.getArgument(0);
            if (name == null || name.isBlank()) {
                return WorkflowProjectLookupResult.notFound("Project not found");
            }
            return WorkflowProjectLookupResult.found(name.trim().toLowerCase(Locale.ROOT), name.trim(), Path.of("D:/repo").toAbsolutePath().normalize());
        });
        return registry;
    }

    private WorkflowProjectRegistryService registryNotFound() {
        WorkflowProjectRegistryService registry = mock(WorkflowProjectRegistryService.class);
        when(registry.lookup(anyString())).thenReturn(WorkflowProjectLookupResult.notFound("Project not found"));
        return registry;
    }

    @Test
    void startRunCompletesSessionWhenWorkflowReturnsContinue() {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        WorkflowTelegramRunTargetResolver resolver = new WorkflowTelegramRunTargetResolver();
        AnalysisReviewWorkflowRunner runner = mock(AnalysisReviewWorkflowRunner.class);
        when(runner.runAnalysisReviewWithOptionalCorrection(any()))
                .thenReturn(new WorkflowStepResult(
                        WorkflowStepDecision.CONTINUE,
                        "done",
                        Map.of("workflowSummaryPath", "data/rapport/v1.1/CodexTime/Phase7/etape1/workflow-summary.md")
                ));

        ExecutorService executor = new DirectExecutorService();
        WorkflowTelegramOrchestrationService service = new WorkflowTelegramOrchestrationService(
                store,
                resolver,
                runner,
                new WorkflowTelegramErrorSanitizer(),
                registryFound(),
                executor,
                Duration.ofMinutes(15),
                Path.of("data/rapport").toAbsolutePath().normalize(),
                "v1.1"
        );

        String response = service.startRun(10L, 20L, "Toolkit", 7, 1);

        assertThat(response).contains("Workflow started");

        WorkflowTelegramSession session = store.getOrCreate(10L);
        assertThat(session.running()).isFalse();
        assertThat(session.lastStatus()).isEqualTo(WorkflowTelegramRunStatus.COMPLETED);
        assertThat(session.lastSummaryPath()).isNotNull();
    }

    @Test
    void startRunInjectsCodexWorkingDirectoryInContext() {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        WorkflowTelegramRunTargetResolver resolver = new WorkflowTelegramRunTargetResolver();
        AnalysisReviewWorkflowRunner runner = mock(AnalysisReviewWorkflowRunner.class);
        when(runner.runAnalysisReviewWithOptionalCorrection(any()))
                .thenReturn(new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "done", Map.of()));

        ExecutorService executor = new DirectExecutorService();
        WorkflowTelegramOrchestrationService service = new WorkflowTelegramOrchestrationService(
                store,
                resolver,
                runner,
                new WorkflowTelegramErrorSanitizer(),
                registryFound(),
                executor,
                Duration.ofMinutes(15),
                Path.of("data/rapport").toAbsolutePath().normalize(),
                "v1.1"
        );

        service.startRun(10L, 20L, "Toolkit", 7, 1);

        ArgumentCaptor<fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowExecutionContext> captor =
                ArgumentCaptor.forClass(fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowExecutionContext.class);
        verify(runner, times(1)).runAnalysisReviewWithOptionalCorrection(captor.capture());
        Object value = captor.getValue().variables().get("codexWorkingDirectory");
        assertThat(value).isInstanceOf(Path.class);
        assertThat((Path) value).isEqualTo(Path.of("D:/repo").toAbsolutePath().normalize());
    }

    @Test
    void startRunUsesSessionProjectPathAndInjectsCodexWorkingDirectory() {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        WorkflowTelegramRunTargetResolver resolver = new WorkflowTelegramRunTargetResolver();
        AnalysisReviewWorkflowRunner runner = mock(AnalysisReviewWorkflowRunner.class);
        when(runner.runAnalysisReviewWithOptionalCorrection(any()))
                .thenReturn(new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "done", Map.of()));

        Path projectPath = tempDir.resolve("project").toAbsolutePath().normalize();
        store.updateContext(10L, 20L, "Toolkit", projectPath, null, null, null);

        ExecutorService executor = new DirectExecutorService();
        WorkflowTelegramOrchestrationService service = new WorkflowTelegramOrchestrationService(
                store,
                resolver,
                runner,
                new WorkflowTelegramErrorSanitizer(),
                registryNotFound(),
                executor,
                Duration.ofMinutes(15),
                Path.of("data/rapport").toAbsolutePath().normalize(),
                "v1.1"
        );

        service.startRun(10L, 20L, (String) null, 7, 1);

        ArgumentCaptor<fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowExecutionContext> captor =
                ArgumentCaptor.forClass(fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowExecutionContext.class);
        verify(runner, times(1)).runAnalysisReviewWithOptionalCorrection(captor.capture());
        assertThat(captor.getValue().variables().get("codexWorkingDirectory")).isEqualTo(projectPath);
        assertThat(captor.getValue().variables()).containsKey("analysisSourcePath");
    }

    @Test
    void startRunMessageContainsWorkflowStartedWithoutEmojiDependency() {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        WorkflowTelegramRunTargetResolver resolver = new WorkflowTelegramRunTargetResolver();
        AnalysisReviewWorkflowRunner runner = mock(AnalysisReviewWorkflowRunner.class);
        when(runner.runAnalysisReviewWithOptionalCorrection(any()))
                .thenReturn(new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "done", Map.of()));

        ExecutorService executor = new DirectExecutorService();
        WorkflowTelegramOrchestrationService service = new WorkflowTelegramOrchestrationService(
                store,
                resolver,
                runner,
                new WorkflowTelegramErrorSanitizer(),
                registryFound(),
                executor,
                Duration.ofMinutes(15),
                Path.of("data/rapport").toAbsolutePath().normalize(),
                "v1.1"
        );

        String response = service.startRun(10L, 20L, "Toolkit", 7, 1);

        assertThat(response).contains("Workflow started");
    }

    @Test
    void startRunRefusesWhenProjectIsUnknown() {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        WorkflowTelegramRunTargetResolver resolver = new WorkflowTelegramRunTargetResolver();
        AnalysisReviewWorkflowRunner runner = mock(AnalysisReviewWorkflowRunner.class);

        ExecutorService executor = new DirectExecutorService();
        WorkflowTelegramOrchestrationService service = new WorkflowTelegramOrchestrationService(
                store,
                resolver,
                runner,
                new WorkflowTelegramErrorSanitizer(),
                registryNotFound(),
                executor,
                Duration.ofMinutes(15),
                Path.of("data/rapport").toAbsolutePath().normalize(),
                "v1.1"
        );

        String response = service.startRun(10L, 20L, "Toolkit", 7, 1);

        assertThat(response).contains("Action impossible");
        assertThat(response).contains("Unknown project");
        assertThat(response).contains("/workflow_project_set");
    }

    @Test
    void startRunRefusesWhenNoProjectConfiguredInSessionOrArgs() {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        WorkflowTelegramRunTargetResolver resolver = new WorkflowTelegramRunTargetResolver();
        AnalysisReviewWorkflowRunner runner = mock(AnalysisReviewWorkflowRunner.class);

        ExecutorService executor = new DirectExecutorService();
        WorkflowTelegramOrchestrationService service = new WorkflowTelegramOrchestrationService(
                store,
                resolver,
                runner,
                new WorkflowTelegramErrorSanitizer(),
                registryFound(),
                executor,
                Duration.ofMinutes(15),
                Path.of("data/rapport").toAbsolutePath().normalize(),
                "v1.1"
        );

        String response = service.startRun(10L, 20L, null, 7, 1);

        assertThat(response).contains("Action impossible");
        assertThat(response).contains("No project configured");
        assertThat(response).contains("/workflow_project_set");
    }

    @Test
    void startRunPreventsDoubleRunWhenSessionIsRunning() {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        WorkflowTelegramRunTargetResolver resolver = new WorkflowTelegramRunTargetResolver();
        AnalysisReviewWorkflowRunner runner = mock(AnalysisReviewWorkflowRunner.class);
        when(runner.runAnalysisReviewWithOptionalCorrection(any()))
                .thenReturn(new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "done", Map.of()));

        ManualExecutorService executor = new ManualExecutorService();
        WorkflowTelegramOrchestrationService service = new WorkflowTelegramOrchestrationService(
                store,
                resolver,
                runner,
                new WorkflowTelegramErrorSanitizer(),
                registryFound(),
                executor,
                Duration.ofMinutes(15),
                Path.of("data/rapport").toAbsolutePath().normalize(),
                "v1.1"
        );

        String first = service.startRun(10L, 20L, "Toolkit", 7, 1);
        String second = service.startRun(10L, 20L, "Toolkit", 7, 1);

        assertThat(first).contains("Workflow started");
        assertThat(second).contains(WorkflowTelegramOrchestrationService.ALREADY_RUNNING_MESSAGE);
    }

    @Test
    void timeoutMarksFailedAndLateCompletionCannotOverwrite() throws Exception {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        WorkflowTelegramRunTargetResolver resolver = new WorkflowTelegramRunTargetResolver();
        AnalysisReviewWorkflowRunner runner = mock(AnalysisReviewWorkflowRunner.class);

        // Do not execute the task at all: force the timeout path.
        ExecutorService executor = new ManualExecutorService();
        WorkflowTelegramOrchestrationService service = new WorkflowTelegramOrchestrationService(
                store,
                resolver,
                runner,
                new WorkflowTelegramErrorSanitizer(),
                registryFound(),
                executor,
                Duration.ofMillis(20),
                Path.of("data/rapport").toAbsolutePath().normalize(),
                "v1.1"
        );

        service.startRun(10L, 20L, "Toolkit", 7, 1);
        String runId = store.getOrCreate(10L).lastRunId();

        // Wait for the timeout to fire.
        Thread.sleep(80);

        WorkflowTelegramSession afterTimeout = store.getOrCreate(10L);
        assertThat(afterTimeout.running()).isFalse();
        assertThat(afterTimeout.lastStatus()).isEqualTo(WorkflowTelegramRunStatus.FAILED);

        // Try to overwrite with a late completion: must be ignored.
        store.completeRun(10L, runId, WorkflowTelegramRunStatus.COMPLETED, Path.of("x"), "done");
        WorkflowTelegramSession afterLateCompletion = store.getOrCreate(10L);
        assertThat(afterLateCompletion.lastStatus()).isEqualTo(WorkflowTelegramRunStatus.FAILED);
    }

    @Test
    void statusReturnsInvalidRequestWhenChatIdMissing() {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        WorkflowTelegramRunTargetResolver resolver = new WorkflowTelegramRunTargetResolver();
        AnalysisReviewWorkflowRunner runner = mock(AnalysisReviewWorkflowRunner.class);

        ExecutorService executor = new DirectExecutorService();
        WorkflowTelegramOrchestrationService service = new WorkflowTelegramOrchestrationService(
                store,
                resolver,
                runner,
                new WorkflowTelegramErrorSanitizer(),
                executor,
                Duration.ofMinutes(15),
                Path.of("data/rapport").toAbsolutePath().normalize(),
                "v1.1"
        );

        assertThat(service.status(null)).contains(WorkflowTelegramOrchestrationService.INVALID_REQUEST_MESSAGE);
    }

    @Test
    void resumeStartsOnlyWhenWaitingHumanAndCompletesOnContinue() {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        WorkflowTelegramRunTargetResolver resolver = new WorkflowTelegramRunTargetResolver();
        AnalysisReviewWorkflowRunner runner = mock(AnalysisReviewWorkflowRunner.class);
        when(runner.runCorrectionAfterReview(any()))
                .thenReturn(new WorkflowStepResult(
                        WorkflowStepDecision.CONTINUE,
                        "resumed",
                        Map.of("workflowSummaryPath", "data/rapport/v1.1/CodexTime/Phase7/etape4/workflow-summary.md")
                ));

        ExecutorService executor = new DirectExecutorService();
        WorkflowTelegramOrchestrationService service = new WorkflowTelegramOrchestrationService(
                store,
                resolver,
                runner,
                new WorkflowTelegramErrorSanitizer(),
                registryFound(),
                executor,
                Duration.ofMinutes(15),
                Path.of("data/rapport").toAbsolutePath().normalize(),
                "v1.1"
        );

        store.updateContext(10L, 20L, "Toolkit", null, 7, 4, null);
        store.tryMarkRunning(10L, "run-1");
        store.completeRun(10L, "run-1", WorkflowTelegramRunStatus.WAITING_HUMAN, Path.of("x"), "wait");

        String response = service.resume(10L, 20L);
        assertThat(response).contains("Action effectuee");
        assertThat(response).contains("Resume started");

        WorkflowTelegramSession session = store.getOrCreate(10L);
        assertThat(session.running()).isFalse();
        assertThat(session.lastStatus()).isEqualTo(WorkflowTelegramRunStatus.COMPLETED);

        ArgumentCaptor<fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowExecutionContext> captor =
                ArgumentCaptor.forClass(fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowExecutionContext.class);
        verify(runner, times(1)).runCorrectionAfterReview(captor.capture());
        Object value = captor.getValue().variables().get("codexWorkingDirectory");
        assertThat(value).isInstanceOf(Path.class);
        assertThat((Path) value).isEqualTo(Path.of("D:/repo").toAbsolutePath().normalize());
        assertThat(captor.getValue().variables()).doesNotContainKey("analysisSourcePath");
    }

    @Test
    void resumeRefusesWhenProjectIsUnknown() {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        WorkflowTelegramRunTargetResolver resolver = new WorkflowTelegramRunTargetResolver();
        AnalysisReviewWorkflowRunner runner = mock(AnalysisReviewWorkflowRunner.class);

        ExecutorService executor = new DirectExecutorService();
        WorkflowTelegramOrchestrationService service = new WorkflowTelegramOrchestrationService(
                store,
                resolver,
                runner,
                new WorkflowTelegramErrorSanitizer(),
                registryNotFound(),
                executor,
                Duration.ofMinutes(15),
                Path.of("data/rapport").toAbsolutePath().normalize(),
                "v1.1"
        );

        store.updateContext(10L, 20L, "Toolkit", null, 7, 4, null);
        store.tryMarkRunning(10L, "run-1");
        store.completeRun(10L, "run-1", WorkflowTelegramRunStatus.WAITING_HUMAN, Path.of("x"), "wait");

        String response = service.resume(10L, 20L);

        assertThat(response).contains("Action impossible");
        assertThat(response).contains("Unknown project");
        assertThat(response).contains("/workflow_project_set");
    }

    @Test
    void resumeRefusesWhenNotWaitingHuman() {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        WorkflowTelegramRunTargetResolver resolver = new WorkflowTelegramRunTargetResolver();
        AnalysisReviewWorkflowRunner runner = mock(AnalysisReviewWorkflowRunner.class);

        ExecutorService executor = new DirectExecutorService();
        WorkflowTelegramOrchestrationService service = new WorkflowTelegramOrchestrationService(
                store,
                resolver,
                runner,
                new WorkflowTelegramErrorSanitizer(),
                executor,
                Duration.ofMinutes(15),
                Path.of("data/rapport").toAbsolutePath().normalize(),
                "v1.1"
        );

        assertThat(service.resume(10L, 20L)).contains(WorkflowTelegramOrchestrationService.NO_WAITING_HUMAN_MESSAGE);

        store.updateContext(10L, 20L, "Toolkit", null, 7, 4, null);
        store.tryMarkRunning(10L, "run-1");
        store.failRun(10L, "run-1", "boom");
        assertThat(service.resume(10L, 20L)).contains(WorkflowTelegramOrchestrationService.NO_WAITING_HUMAN_MESSAGE);

        store.tryMarkRunning(11L, "run-11");
        assertThat(service.resume(11L, 20L)).contains(WorkflowTelegramOrchestrationService.ALREADY_RUNNING_MESSAGE);
    }

    @Test
    void resumeRefusesWhenContextIncomplete() {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        WorkflowTelegramRunTargetResolver resolver = new WorkflowTelegramRunTargetResolver();
        AnalysisReviewWorkflowRunner runner = mock(AnalysisReviewWorkflowRunner.class);

        ExecutorService executor = new DirectExecutorService();
        WorkflowTelegramOrchestrationService service = new WorkflowTelegramOrchestrationService(
                store,
                resolver,
                runner,
                new WorkflowTelegramErrorSanitizer(),
                executor,
                Duration.ofMinutes(15),
                Path.of("data/rapport").toAbsolutePath().normalize(),
                "v1.1"
        );

        store.tryMarkRunning(10L, "run-1");
        store.completeRun(10L, "run-1", WorkflowTelegramRunStatus.WAITING_HUMAN, Path.of("x"), "wait");

        assertThat(service.resume(10L, 20L)).contains(WorkflowTelegramOrchestrationService.INCOMPLETE_CONTEXT_MESSAGE);
    }

    @Test
    void resumePreventsDoubleResumeWhenSessionAlreadyRunning() {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        WorkflowTelegramRunTargetResolver resolver = new WorkflowTelegramRunTargetResolver();
        AnalysisReviewWorkflowRunner runner = mock(AnalysisReviewWorkflowRunner.class);
        when(runner.runCorrectionAfterReview(any()))
                .thenReturn(new WorkflowStepResult(WorkflowStepDecision.CONTINUE, "done", Map.of()));

        ExecutorService executor = new ManualExecutorService();
        WorkflowTelegramOrchestrationService service = new WorkflowTelegramOrchestrationService(
                store,
                resolver,
                runner,
                new WorkflowTelegramErrorSanitizer(),
                registryFound(),
                executor,
                Duration.ofMinutes(15),
                Path.of("data/rapport").toAbsolutePath().normalize(),
                "v1.1"
        );

        store.updateContext(10L, 20L, "Toolkit", null, 7, 4, null);
        store.tryMarkRunning(10L, "run-1");
        store.completeRun(10L, "run-1", WorkflowTelegramRunStatus.WAITING_HUMAN, Path.of("x"), "wait");

        assertThat(service.resume(10L, 20L)).contains("Resume started");
        assertThat(service.resume(10L, 20L)).contains(WorkflowTelegramOrchestrationService.ALREADY_RUNNING_MESSAGE);
    }

    @Test
    void resumeMapsWaitHumanAndStopFailure() {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        WorkflowTelegramRunTargetResolver resolver = new WorkflowTelegramRunTargetResolver();
        AnalysisReviewWorkflowRunner runner = mock(AnalysisReviewWorkflowRunner.class);

        ExecutorService executor = new DirectExecutorService();
        WorkflowTelegramOrchestrationService service = new WorkflowTelegramOrchestrationService(
                store,
                resolver,
                runner,
                new WorkflowTelegramErrorSanitizer(),
                registryFound(),
                executor,
                Duration.ofMinutes(15),
                Path.of("data/rapport").toAbsolutePath().normalize(),
                "v1.1"
        );

        store.updateContext(10L, 20L, "Toolkit", null, 7, 4, null);
        store.tryMarkRunning(10L, "run-1");
        store.completeRun(10L, "run-1", WorkflowTelegramRunStatus.WAITING_HUMAN, Path.of("x"), "wait");

        when(runner.runCorrectionAfterReview(any()))
                .thenReturn(new WorkflowStepResult(WorkflowStepDecision.WAIT_HUMAN, "still waiting", Map.of()));
        service.resume(10L, 20L);
        assertThat(store.getOrCreate(10L).lastStatus()).isEqualTo(WorkflowTelegramRunStatus.WAITING_HUMAN);

        store.updateContext(12L, 20L, "Toolkit", null, 7, 4, null);
        store.tryMarkRunning(12L, "run-12");
        store.completeRun(12L, "run-12", WorkflowTelegramRunStatus.WAITING_HUMAN, Path.of("x"), "wait");

        when(runner.runCorrectionAfterReview(any()))
                .thenReturn(new WorkflowStepResult(
                        WorkflowStepDecision.STOP_FAILURE,
                        "fail at /home/bob/secret.txt\n\tat com.acme.Foo.bar(Foo.java:12)",
                        Map.of()
                ));
        service.resume(12L, 20L);
        WorkflowTelegramSession failed = store.getOrCreate(12L);
        assertThat(failed.lastStatus()).isEqualTo(WorkflowTelegramRunStatus.FAILED);
        assertThat(failed.lastError()).doesNotContain("/home/bob/secret.txt");
        assertThat(failed.lastError()).doesNotContain("\tat");
    }

    @Test
    void runnerRateLimitIsSanitizedToFriendlyMessage() {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        WorkflowTelegramRunTargetResolver resolver = new WorkflowTelegramRunTargetResolver();
        AnalysisReviewWorkflowRunner runner = mock(AnalysisReviewWorkflowRunner.class);
        when(runner.runAnalysisReviewWithOptionalCorrection(any()))
                .thenReturn(new WorkflowStepResult(WorkflowStepDecision.STOP_FAILURE, "429 Too Many Requests", Map.of()));

        WorkflowTelegramOrchestrationService service = new WorkflowTelegramOrchestrationService(
                store,
                resolver,
                runner,
                new WorkflowTelegramErrorSanitizer(),
                registryFound(),
                new DirectExecutorService(),
                Duration.ofMinutes(15),
                Path.of("data/rapport").toAbsolutePath().normalize(),
                "v1.1"
        );

        service.startRun(10L, 20L, "Toolkit", 7, 1);

        WorkflowTelegramSession session = store.getOrCreate(10L);
        assertThat(session.lastStatus()).isEqualTo(WorkflowTelegramRunStatus.FAILED);
        assertThat(session.lastError()).contains("Provider quota or rate limit reached");
        assertThat(service.status(10L)).doesNotContain("429");
    }

    @Test
    void runnerExceptionMessageIsSanitizedBeforeStoring() {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        WorkflowTelegramRunTargetResolver resolver = new WorkflowTelegramRunTargetResolver();
        AnalysisReviewWorkflowRunner runner = mock(AnalysisReviewWorkflowRunner.class);
        when(runner.runAnalysisReviewWithOptionalCorrection(any()))
                .thenThrow(new RuntimeException("boom\n\tat com.acme.Foo.bar(Foo.java:12)"));

        WorkflowTelegramOrchestrationService service = new WorkflowTelegramOrchestrationService(
                store,
                resolver,
                runner,
                new WorkflowTelegramErrorSanitizer(),
                registryFound(),
                new DirectExecutorService(),
                Duration.ofMinutes(15),
                Path.of("data/rapport").toAbsolutePath().normalize(),
                "v1.1"
        );

        service.startRun(10L, 20L, "Toolkit", 7, 1);

        WorkflowTelegramSession session = store.getOrCreate(10L);
        assertThat(session.lastStatus()).isEqualTo(WorkflowTelegramRunStatus.FAILED);
        assertThat(session.lastError()).contains("boom");
        assertThat(session.lastError()).doesNotContain("\tat");
        assertThat(session.lastError()).doesNotContain("Foo.java");
    }

    @Test
    void fullCycleWaitHumanThenResumeCompletesAndLateCompletionIsIgnored() throws Exception {
        Path sharedRoot = tempDir.resolve("shared");
        WorkspaceLayout workspaceLayout = workspaceLayout(sharedRoot);

        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        WorkflowTelegramRoadmapService roadmapService = new WorkflowTelegramRoadmapService(workspaceLayout, store);

        AnalysisReviewWorkflowRunner runner = mock(AnalysisReviewWorkflowRunner.class);
        Path summaryPath = Path.of("data/rapport/v1.1/CodexTime/Phase7/etape8/workflow-summary.md");
        when(runner.runAnalysisReviewWithOptionalCorrection(any()))
                .thenReturn(new WorkflowStepResult(
                        WorkflowStepDecision.WAIT_HUMAN,
                        "need human",
                        Map.of("workflowSummaryPath", summaryPath.toString())
                ));
        when(runner.runCorrectionAfterReview(any()))
                .thenReturn(new WorkflowStepResult(
                        WorkflowStepDecision.CONTINUE,
                        "resumed ok",
                        Map.of("workflowSummaryPath", summaryPath.toString())
                ));

        WorkflowTelegramOrchestrationService orchestration = new WorkflowTelegramOrchestrationService(
                store,
                new WorkflowTelegramRunTargetResolver(),
                runner,
                new WorkflowTelegramErrorSanitizer(),
                registryFound(),
                new DirectExecutorService(),
                Duration.ofMinutes(15),
                Path.of("data/rapport").toAbsolutePath().normalize(),
                "v1.1"
        );

        Path roadmap = sharedRoot.resolve("projects/Toolkit/roadmaps/phase7.md");
        java.nio.file.Files.createDirectories(roadmap.getParent());
        java.nio.file.Files.writeString(roadmap, "# Roadmap\n", java.nio.charset.StandardCharsets.UTF_8);
        String roadmapResponse = roadmapService.loadRoadmap(10L, 20L, "Toolkit", "projects/Toolkit/roadmaps/phase7.md");
        assertThat(roadmapResponse).contains("Action effectuee");

        String runResponse = orchestration.startRun(10L, 20L, "Toolkit", 7, 8);
        assertThat(runResponse).contains("Workflow started");

        WorkflowTelegramSession afterRun = store.getOrCreate(10L);
        assertThat(afterRun.lastStatus()).isEqualTo(WorkflowTelegramRunStatus.WAITING_HUMAN);
        assertThat(afterRun.running()).isFalse();
        assertThat(afterRun.lastSummaryPath()).isEqualTo(summaryPath);
        String runId1 = afterRun.lastRunId();

        String resumeResponse = orchestration.resume(10L, 20L);
        assertThat(resumeResponse).contains("Resume started");

        WorkflowTelegramSession afterResume = store.getOrCreate(10L);
        assertThat(afterResume.lastStatus()).isEqualTo(WorkflowTelegramRunStatus.COMPLETED);
        assertThat(afterResume.running()).isFalse();
        assertThat(afterResume.lastSummaryPath()).isEqualTo(summaryPath);
        String runId2 = afterResume.lastRunId();
        assertThat(runId2).isNotBlank();
        assertThat(runId2).isNotEqualTo(runId1);

        // Late completion from the first run must not overwrite the completed state.
        store.completeRun(10L, runId1, WorkflowTelegramRunStatus.COMPLETED, Path.of("x"), "late");
        assertThat(store.getOrCreate(10L).lastStatus()).isEqualTo(WorkflowTelegramRunStatus.COMPLETED);
    }

    @Test
    void runStopFailureMarksFailedWithSanitizedError() {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        WorkflowTelegramRunTargetResolver resolver = new WorkflowTelegramRunTargetResolver();
        AnalysisReviewWorkflowRunner runner = mock(AnalysisReviewWorkflowRunner.class);
        when(runner.runAnalysisReviewWithOptionalCorrection(any()))
                .thenReturn(new WorkflowStepResult(
                        WorkflowStepDecision.STOP_FAILURE,
                        "fail at /home/user/project/secret/file.md\n\tat com.acme.Foo.bar(Foo.java:12)",
                        Map.of()
                ));

        WorkflowTelegramOrchestrationService service = new WorkflowTelegramOrchestrationService(
                store,
                resolver,
                runner,
                new WorkflowTelegramErrorSanitizer(),
                registryFound(),
                new DirectExecutorService(),
                Duration.ofMinutes(15),
                Path.of("data/rapport").toAbsolutePath().normalize(),
                "v1.1"
        );

        service.startRun(10L, 20L, "Toolkit", 7, 8);

        WorkflowTelegramSession session = store.getOrCreate(10L);
        assertThat(session.lastStatus()).isEqualTo(WorkflowTelegramRunStatus.FAILED);
        assertThat(session.lastError()).contains("fail");
        assertThat(session.lastError()).doesNotContain(UNIX_ABSOLUTE_PATH_MARKER);
        assertThat(session.lastError()).doesNotContain("\tat");

        String status = service.status(10L);
        assertThat(status).doesNotContain(UNIX_ABSOLUTE_PATH_MARKER);
        assertThat(status).doesNotContain("\tat");
    }

    @Test
    void workflowHomeRendersWithoutActiveContext() {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        WorkflowTelegramOrchestrationService service = new WorkflowTelegramOrchestrationService(
                store,
                new WorkflowTelegramRunTargetResolver(),
                mock(AnalysisReviewWorkflowRunner.class),
                new WorkflowTelegramErrorSanitizer(),
                new DirectExecutorService(),
                Duration.ofMinutes(15),
                Path.of("data/rapport").toAbsolutePath().normalize(),
                "v1.1"
        );

        String response = service.workflowHome(10L);

        assertThat(response).contains("Workflow assistant");
        assertThat(response).contains("Status: IDLE");
        assertThat(response).contains("Roadmap: missing");
        assertThat(response).contains("Next:");
        assertThat(response).contains("/workflow_roadmap_load");
        assertThat(response).contains("/workflow_run");
        assertThat(response).contains("/workflow_status");
        assertThat(response).contains("/workflow_summary");
        assertThat(response).contains("/workflow_resume");
        assertThat(response).doesNotContain(WINDOWS_ABSOLUTE_PATH_MARKER);
        assertThat(response).doesNotContainIgnoringCase("token");
        assertThat(response).doesNotContain("Exception");
        assertThat(response).doesNotContain("\tat ");
        assertThat(response).doesNotContain(UNIX_ABSOLUTE_PATH_MARKER);
    }

    @Test
    void workflowHomeRendersWithActiveContext() {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        Path roadmapPath = Path.of("workspace/shared/roadmap.md");
        store.updateContext(10L, 20L, "Toolkit", null, 7, 5, roadmapPath);
        WorkflowTelegramOrchestrationService service = new WorkflowTelegramOrchestrationService(
                store,
                new WorkflowTelegramRunTargetResolver(),
                mock(AnalysisReviewWorkflowRunner.class),
                new WorkflowTelegramErrorSanitizer(),
                new DirectExecutorService(),
                Duration.ofMinutes(15),
                Path.of("data/rapport").toAbsolutePath().normalize(),
                "v1.1"
        );

        String response = service.workflowHome(10L);

        assertThat(response).contains("Project: Toolkit");
        assertThat(response).contains("Phase: 7");
        assertThat(response).contains("Etape: 5");
        assertThat(response).contains("Roadmap: loaded");
        assertThat(response).doesNotContain(roadmapPath.toString());
        assertThat(response).contains("Next:");
        assertThat(response).contains("/workflow_run");
        assertThat(response).doesNotContain(WINDOWS_ABSOLUTE_PATH_MARKER);
        assertThat(response).doesNotContain(UNIX_ABSOLUTE_PATH_MARKER);
    }

    @Test
    void statusRendersIdleRunningWaitingHumanAndFailedStates() {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        WorkflowTelegramOrchestrationService service = new WorkflowTelegramOrchestrationService(
                store,
                new WorkflowTelegramRunTargetResolver(),
                mock(AnalysisReviewWorkflowRunner.class),
                new WorkflowTelegramErrorSanitizer(),
                new DirectExecutorService(),
                Duration.ofMinutes(15),
                Path.of("data/rapport").toAbsolutePath().normalize(),
                "v1.1"
        );

        String idle = service.status(1L);
        assertThat(idle).contains("Status: IDLE");
        assertThat(idle).contains("Next: /workflow_roadmap_load");
        assertThat(idle).doesNotContain(WINDOWS_ABSOLUTE_PATH_MARKER);
        assertThat(idle).doesNotContain(UNIX_ABSOLUTE_PATH_MARKER);

        store.updateContext(2L, 20L, "Toolkit", null, 7, 5, Path.of("workspace/shared/roadmap.md"));
        store.tryMarkRunning(2L, "run-2");
        String running = service.status(2L);
        assertThat(running).contains("Status: RUNNING");
        assertThat(running).contains("Run: run-2");
        assertThat(running).contains("Next: /workflow_status");
        assertThat(running).doesNotContain(WINDOWS_ABSOLUTE_PATH_MARKER);
        assertThat(running).doesNotContain(UNIX_ABSOLUTE_PATH_MARKER);

        Path summaryPath = Path.of("data/rapport/workflow-summary.md");
        store.completeRun(2L, "run-2", WorkflowTelegramRunStatus.WAITING_HUMAN, summaryPath, "review needed");
        String waiting = service.status(2L);
        assertThat(waiting).contains("Status: WAITING_HUMAN");
        assertThat(waiting).contains("Summary: available");
        assertThat(waiting).doesNotContain(summaryPath.toString());
        assertThat(waiting).contains("/workflow_summary puis /workflow_resume");
        assertThat(waiting).doesNotContain(WINDOWS_ABSOLUTE_PATH_MARKER);
        assertThat(waiting).doesNotContain(UNIX_ABSOLUTE_PATH_MARKER);

        store.updateContext(3L, 20L, "Toolkit", null, 7, 5, Path.of("workspace/shared/roadmap.md"));
        store.tryMarkRunning(3L, "run-3");
        store.failRun(3L, "run-3", "boom");
        String failed = service.status(3L);
        assertThat(failed).contains("Status: FAILED");
        assertThat(failed).contains("Last error: boom");
        assertThat(failed).contains("/workflow_status puis inspecter l'erreur");
        assertThat(failed).doesNotContain(WINDOWS_ABSOLUTE_PATH_MARKER);
        assertThat(failed).doesNotContain(UNIX_ABSOLUTE_PATH_MARKER);
    }

    private WorkspaceLayout workspaceLayout(Path sharedRoot) {
        WorkspaceProperties properties = new WorkspaceProperties();
        properties.setAgentsRoot(tempDir.resolve("agents").toString());
        properties.setSharedRoot(sharedRoot.toString());
        return new WorkspaceLayout(properties);
    }

    private static final class DirectExecutorService extends AbstractExecutorService {
        private boolean shutdown;

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }

        @Override
        public void execute(Runnable command) {
            command.run();
        }
    }

    private static final class ManualExecutorService extends AbstractExecutorService {
        private boolean shutdown;

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public List<Runnable> shutdownNow() {
            shutdown = true;
            return List.of();
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public boolean isTerminated() {
            return shutdown;
        }

        @Override
        public boolean awaitTermination(long timeout, TimeUnit unit) {
            return true;
        }

        @Override
        public void execute(Runnable command) {
            // Intentionally do not run the command: simulates "async still running"
        }
    }
}
