package fr.ses10doigts.toolkitbridge.service.telegram.workflow;

import fr.ses10doigts.toolkitbridge.config.telegram.workflow.WorkflowTelegramProperties;
import fr.ses10doigts.toolkitbridge.service.agent.definition.AgentDefinitionService;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.AnalysisReviewWorkflowRunner;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.cli.AnalysisReviewWorkflowRunnerFactory;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowExecutionContext;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowRun;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.WorkflowStepDecision;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.WorkflowStepResult;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.project.WorkflowProjectLookupResult;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.project.WorkflowProjectRegistryService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class WorkflowTelegramOrchestrationService {

    static final String INVALID_REQUEST_MESSAGE = "Invalid request";
    static final String ALREADY_RUNNING_MESSAGE = "Workflow already running";
    static final String NO_WAITING_HUMAN_MESSAGE = "Aucun workflow en attente d'action humaine";
    static final String INCOMPLETE_CONTEXT_MESSAGE = "Contexte workflow incomplet";
    static final String NO_PROJECT_MESSAGE = "No project configured for this workflow session";
    static final String PROJECT_PATH_MISSING_MESSAGE = "Project path is missing for this workflow session";
    static final String TRY_PROJECT_SET = "/workflow_project_set name=ToolkitBridge path=\"<project-path>\"";

    private static final String DEFAULT_WORKFLOW_TYPE = "analysis-review";
    private static final String DEFAULT_TARGET_STEP_REF = "external";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(15);

    private final WorkflowTelegramSessionStore sessionStore;
    private final WorkflowTelegramRunTargetResolver runTargetResolver;
    private final AnalysisReviewWorkflowRunner runner;
    private final WorkflowTelegramErrorSanitizer errorSanitizer;
    private final AgentDefinitionService agentDefinitionService;
    private final WorkflowProjectRegistryService projectRegistryService;
    private final ExecutorService executor;
    private final Duration timeout;
    private final Path reportRootDirectory;
    private final String reportVersion;

    @Autowired
    public WorkflowTelegramOrchestrationService(WorkflowTelegramSessionStore sessionStore,
                                               WorkflowTelegramRunTargetResolver runTargetResolver,
                                               WorkflowTelegramErrorSanitizer errorSanitizer,
                                               AgentDefinitionService agentDefinitionService,
                                               WorkflowProjectRegistryService projectRegistryService,
                                               WorkflowTelegramProperties properties,
                                               ExecutorService workflowExecutor) {
        this(
                sessionStore,
                runTargetResolver,
                AnalysisReviewWorkflowRunnerFactory.createDefault(),
                errorSanitizer,
                agentDefinitionService,
                projectRegistryService,
                workflowExecutor,
                properties == null ? DEFAULT_TIMEOUT : Duration.ofMinutes(properties.getTimeoutMinutes()),
                properties == null
                        ? Path.of(".").toAbsolutePath().normalize().resolve("data/rapport").normalize()
                        : Path.of(properties.getReportRootDirectory()).toAbsolutePath().normalize(),
                properties == null ? "v1.1" : properties.getReportVersion()
        );
    }

    WorkflowTelegramOrchestrationService(WorkflowTelegramSessionStore sessionStore,
                                         WorkflowTelegramRunTargetResolver runTargetResolver,
                                         AnalysisReviewWorkflowRunner runner,
                                         WorkflowTelegramErrorSanitizer errorSanitizer,
                                         AgentDefinitionService agentDefinitionService,
                                         WorkflowProjectRegistryService projectRegistryService,
                                         ExecutorService executor,
                                         Duration timeout,
                                         Path reportRootDirectory,
                                         String reportVersion) {
        this.sessionStore = sessionStore;
        this.runTargetResolver = runTargetResolver;
        this.runner = runner;
        this.errorSanitizer = errorSanitizer == null ? new WorkflowTelegramErrorSanitizer() : errorSanitizer;
        this.agentDefinitionService = agentDefinitionService;
        this.projectRegistryService = projectRegistryService;
        this.executor = executor;
        this.timeout = timeout == null ? DEFAULT_TIMEOUT : timeout;
        this.reportRootDirectory = reportRootDirectory;
        this.reportVersion = reportVersion == null ? "v1.1" : reportVersion;
    }

    WorkflowTelegramOrchestrationService(WorkflowTelegramSessionStore sessionStore,
                                         WorkflowTelegramRunTargetResolver runTargetResolver,
                                         AnalysisReviewWorkflowRunner runner,
                                         WorkflowTelegramErrorSanitizer errorSanitizer,
                                         ExecutorService executor,
                                         Duration timeout,
                                         Path reportRootDirectory,
                                         String reportVersion) {
        this(
                sessionStore,
                runTargetResolver,
                runner,
                errorSanitizer,
                null,
                null,
                executor,
                timeout,
                reportRootDirectory,
                reportVersion
        );
    }

    WorkflowTelegramOrchestrationService(WorkflowTelegramSessionStore sessionStore,
                                         WorkflowTelegramRunTargetResolver runTargetResolver,
                                         AnalysisReviewWorkflowRunner runner,
                                         WorkflowTelegramErrorSanitizer errorSanitizer,
                                         WorkflowProjectRegistryService projectRegistryService,
                                         ExecutorService executor,
                                         Duration timeout,
                                         Path reportRootDirectory,
                                         String reportVersion) {
        this(
                sessionStore,
                runTargetResolver,
                runner,
                errorSanitizer,
                null,
                projectRegistryService,
                executor,
                timeout,
                reportRootDirectory,
                reportVersion
        );
    }

    public String startRun(Long chatId, Long userId, String projectName, Integer phase, Integer etape) {
        return startRun(chatId, userId, null, projectName, phase, etape);
    }

    public String startRun(Long chatId, Long userId, String botId, String projectName, Integer phase, Integer etape) {
        if (chatId == null) {
            return WorkflowTelegramMessageRenderer.errorMessage(INVALID_REQUEST_MESSAGE, "/workflow");
        }

        WorkflowTelegramSession session = sessionStore.getOrCreate(chatId);
        boolean explicitProjectArg = projectName != null && !projectName.isBlank();
        WorkflowTelegramRunTarget target = runTargetResolver.resolveRunTarget(session, projectName, phase, etape);
        if (!target.valid()) {
            if (target.errorCode() == WorkflowTelegramRunTargetErrorCode.MISSING_PROJECT) {
                log.warn("Workflow run rejected — no project configured: chatId={}", chatId);
                return WorkflowTelegramMessageRenderer.errorMessage(NO_PROJECT_MESSAGE, TRY_PROJECT_SET);
            }
            log.warn("Workflow run rejected — invalid target: chatId={}", chatId);
            return WorkflowTelegramMessageRenderer.errorMessage(
                    target.errorMessage() == null ? "Invalid target" : target.errorMessage(),
                    "/workflow_status"
            );
        }

        Path resolvedProjectPath = resolveProjectPathForRun(session, explicitProjectArg, target.projectName());
        if (resolvedProjectPath == null) {
            if (explicitProjectArg) {
                log.warn("Workflow run rejected — unknown project: chatId={}, project={}", chatId, target.projectName());
                return WorkflowTelegramMessageRenderer.errorMessage("Unknown project: " + target.projectName(), TRY_PROJECT_SET);
            }
            log.warn("Workflow run rejected — no project configured: chatId={}", chatId);
            return WorkflowTelegramMessageRenderer.errorMessage(NO_PROJECT_MESSAGE, TRY_PROJECT_SET);
        }

        sessionStore.updateContext(chatId, userId, target.projectName(), resolvedProjectPath, target.phase(), target.etape(), null);

        String runId = "tg-" + UUID.randomUUID();
        if (!sessionStore.tryMarkRunning(chatId, runId)) {
            log.warn("Workflow run rejected — already running: chatId={}", chatId);
            return WorkflowTelegramMessageRenderer.errorMessage(ALREADY_RUNNING_MESSAGE, "/workflow_status");
        }

        log.info("Workflow run started: chatId={}, runId={}, project={}, phase={}, etape={}",
                chatId, runId, target.projectName(), target.phase(), target.etape());
        Path roadmapPath = session.roadmapPath();
        executeAsync(chatId, runId, () -> executeRun(chatId, runId, userId, botId, target, roadmapPath, resolvedProjectPath));

        return WorkflowTelegramMessageRenderer.successMessage(
                "Workflow started for project=" + target.projectName()
                        + ", phase=" + target.phase()
                        + ", etape=" + target.etape()
                        + ", runId=" + runId,
                "/workflow_status"
        );
    }

    public String resume(Long chatId, Long userId) {
        if (chatId == null) {
            return WorkflowTelegramMessageRenderer.errorMessage(INVALID_REQUEST_MESSAGE, "/workflow");
        }

        WorkflowTelegramSession session = sessionStore.getOrCreate(chatId);
        if (session.running() || session.lastStatus() == WorkflowTelegramRunStatus.RUNNING) {
            log.warn("Workflow resume rejected — already running: chatId={}", chatId);
            return WorkflowTelegramMessageRenderer.errorMessage(ALREADY_RUNNING_MESSAGE, "/workflow_status");
        }
        if (session.lastStatus() != WorkflowTelegramRunStatus.WAITING_HUMAN) {
            log.warn("Workflow resume rejected — not in WAIT_HUMAN state: chatId={}, status={}", chatId, session.lastStatus());
            return WorkflowTelegramMessageRenderer.errorMessage(NO_WAITING_HUMAN_MESSAGE, "/workflow_summary");
        }
        if (session.phase() == null || session.etape() == null) {
            log.warn("Workflow resume rejected — incomplete context: chatId={}", chatId);
            return WorkflowTelegramMessageRenderer.errorMessage(INCOMPLETE_CONTEXT_MESSAGE, "/workflow_status");
        }
        if (session.projectName() == null || session.projectName().isBlank()) {
            log.warn("Workflow resume rejected — no project configured: chatId={}", chatId);
            return WorkflowTelegramMessageRenderer.errorMessage(NO_PROJECT_MESSAGE, TRY_PROJECT_SET);
        }

        Path resolvedProjectPath = resolveProjectPathForResume(session);
        if (resolvedProjectPath == null) {
            if (projectRegistryService != null) {
                WorkflowProjectLookupResult lookup = projectRegistryService.lookup(session.projectName());
                if (lookup == null || !lookup.found()) {
                    log.warn("Workflow resume rejected — unknown project: chatId={}, project={}", chatId, session.projectName());
                    return WorkflowTelegramMessageRenderer.errorMessage("Unknown project: " + session.projectName(), TRY_PROJECT_SET);
                }
            }
            log.warn("Workflow resume rejected — project path missing: chatId={}, project={}", chatId, session.projectName());
            return WorkflowTelegramMessageRenderer.errorMessage(PROJECT_PATH_MISSING_MESSAGE, TRY_PROJECT_SET);
        }
        if (session.projectPath() == null) {
            sessionStore.updateContext(chatId, userId, session.projectName(), resolvedProjectPath, null, null, null);
        }

        WorkflowTelegramRunTarget target = WorkflowTelegramRunTarget.ok(
                session.projectName(),
                session.phase(),
                session.etape()
        );

        String runId = "tg-" + UUID.randomUUID();
        if (!sessionStore.tryMarkRunning(chatId, runId)) {
            log.warn("Workflow resume rejected — already running: chatId={}", chatId);
            return WorkflowTelegramMessageRenderer.errorMessage(ALREADY_RUNNING_MESSAGE, "/workflow_status");
        }

        log.info("Workflow resume started: chatId={}, runId={}, project={}, phase={}, etape={}",
                chatId, runId, session.projectName(), session.phase(), session.etape());
        executeAsync(chatId, runId, () -> executeResume(chatId, runId, userId, target, resolvedProjectPath));

        return WorkflowTelegramMessageRenderer.successMessage(
                "Resume started for project=" + textValue(session.projectName())
                        + ", phase=" + session.phase()
                        + ", etape=" + session.etape()
                        + ", runId=" + runId,
                "/workflow_status"
        );
    }

    private Path resolveProjectPathForRun(WorkflowTelegramSession session, boolean explicitProjectArg, String resolvedProjectName) {
        if (resolvedProjectName == null || resolvedProjectName.isBlank()) {
            return null;
        }

        if (!explicitProjectArg) {
            if (session == null || session.projectName() == null || session.projectName().isBlank()) {
                return null;
            }
            if (session.projectPath() != null && resolvedProjectName.trim().equals(session.projectName())) {
                return session.projectPath();
            }
        }

        if (projectRegistryService == null) {
            return null;
        }
        WorkflowProjectLookupResult lookup = projectRegistryService.lookup(resolvedProjectName);
        if (lookup == null || !lookup.found()) {
            return null;
        }
        return lookup.normalizedProjectPath();
    }

    private Path resolveProjectPathForResume(WorkflowTelegramSession session) {
        if (session == null) {
            return null;
        }
        if (session.projectPath() != null) {
            return session.projectPath();
        }
        if (session.projectName() == null || session.projectName().isBlank()) {
            return null;
        }
        if (projectRegistryService == null) {
            return null;
        }
        WorkflowProjectLookupResult lookup = projectRegistryService.lookup(session.projectName());
        if (lookup == null || !lookup.found()) {
            return null;
        }
        return lookup.normalizedProjectPath();
    }

    public String workflowHome(Long chatId) {
        if (chatId == null) {
            return WorkflowTelegramMessageRenderer.errorMessage(INVALID_REQUEST_MESSAGE, "/workflow");
        }
        return WorkflowTelegramMessageRenderer.workflowHome(sessionStore.getOrCreate(chatId));
    }

    public String status(Long chatId) {
        if (chatId == null) {
            return WorkflowTelegramMessageRenderer.errorMessage(INVALID_REQUEST_MESSAGE, "/workflow");
        }
        return WorkflowTelegramMessageRenderer.statusView(sessionStore.getOrCreate(chatId));
    }

    private void executeRun(Long chatId,
                            String runId,
                            Long userId,
                            String botId,
                            WorkflowTelegramRunTarget target,
                            Path roadmapPath,
                            Path projectPath) {
        try {
            if (projectPath == null) {
                sessionStore.failRun(chatId, runId, "Project path is missing for workflow execution");
                return;
            }
            WorkflowExecutionContext context = buildContext(runId, chatId, userId, botId, target, roadmapPath, projectPath);
            WorkflowStepResult result = runner.runAnalysisReviewWithOptionalCorrection(context);
            handleResult(chatId, runId, result);
        } catch (RuntimeException e) {
            log.error(
                    "Workflow run failed chatId={} userId={} runId={} project={} phase={} etape={}",
                    chatId,
                    userId,
                    runId,
                    target == null ? null : target.projectName(),
                    target == null ? null : target.phase(),
                    target == null ? null : target.etape(),
                    e
            );
            sessionStore.failRun(chatId, runId, sanitizeForSession(e));
        }
    }

    private void executeResume(Long chatId, String runId, Long userId, WorkflowTelegramRunTarget target, Path projectPath) {
        try {
            if (projectPath == null) {
                sessionStore.failRun(chatId, runId, "Project path is missing for workflow execution");
                return;
            }
            WorkflowExecutionContext context = buildResumeContext(runId, chatId, userId, target, projectPath);
            WorkflowStepResult result = runner.runCorrectionAfterReview(context);
            handleResult(chatId, runId, result);
        } catch (RuntimeException e) {
            log.error(
                    "Workflow resume failed chatId={} userId={} runId={} project={} phase={} etape={}",
                    chatId,
                    userId,
                    runId,
                    target == null ? null : target.projectName(),
                    target == null ? null : target.phase(),
                    target == null ? null : target.etape(),
                    e
            );
            sessionStore.failRun(chatId, runId, sanitizeForSession(e));
        }
    }

    private WorkflowExecutionContext buildContext(String runId,
                                                 Long chatId,
                                                 Long userId,
                                                 String botId,
                                                 WorkflowTelegramRunTarget target,
                                                 Path roadmapPath,
                                                 Path projectPath) {
        Instant now = Instant.now();
        WorkflowRun workflowRun = new WorkflowRun(
                runId,
                DEFAULT_WORKFLOW_TYPE,
                DEFAULT_TARGET_STEP_REF,
                null,
                null,
                null,
                now,
                now,
                now,
                null,
                Map.of(
                        "telegram.chatId", chatId,
                        "telegram.userId", userId == null ? "" : userId
                )
        );

        Map<String, Object> variables = new HashMap<>();
        variables.put("reportRootDirectory", reportRootDirectory);
        variables.put("reportVersion", reportVersion);
        variables.put("reportPhase", reportPhaseFolder(target));
        variables.put("stepNumber", target.etape());
        variables.put("analysisSourcePath", analysisSourcePath(target));
        variables.put("codexWorkingDirectory", projectPath);
        if (roadmapPath != null) {
            variables.put("roadmapPath", roadmapPath);
        }
        resolveAgentLlmConfiguration(botId).ifPresent(config -> {
            variables.put("llmProvider", config.llmProvider());
            variables.put("llmModel", config.llmModel());
        });

        return new WorkflowExecutionContext(workflowRun, null, variables);
    }

    private WorkflowExecutionContext buildResumeContext(String runId,
                                                       Long chatId,
                                                       Long userId,
                                                       WorkflowTelegramRunTarget target,
                                                       Path projectPath) {
        Instant now = Instant.now();
        WorkflowRun workflowRun = new WorkflowRun(
                runId,
                DEFAULT_WORKFLOW_TYPE,
                DEFAULT_TARGET_STEP_REF,
                null,
                null,
                null,
                now,
                now,
                now,
                null,
                Map.of(
                        "telegram.chatId", chatId,
                        "telegram.userId", userId == null ? "" : userId
                )
        );

        Map<String, Object> variables = new HashMap<>();
        variables.put("reportRootDirectory", reportRootDirectory);
        variables.put("reportVersion", reportVersion);
        variables.put("reportPhase", reportPhaseFolder(target));
        variables.put("stepNumber", target.etape());
        variables.put("codexWorkingDirectory", projectPath);

        return new WorkflowExecutionContext(workflowRun, null, variables);
    }

    private String reportPhaseFolder(WorkflowTelegramRunTarget target) {
        return "phase" + target.phase() + "/etape" + target.etape();
    }

    private Path analysisSourcePath(WorkflowTelegramRunTarget target) {
        String phaseFolder = reportPhaseFolder(target);
        return reportRootDirectory
                .resolve(reportVersion)
                .resolve(phaseFolder)
                .resolve(target.etape() + ".analysis.md")
                .normalize();
    }

    private void handleResult(Long chatId, String runId, WorkflowStepResult result) {
        if (result == null) {
            log.warn("Workflow returned null result: chatId={}, runId={}", chatId, runId);
            sessionStore.failRun(chatId, runId, "Workflow returned no result");
            return;
        }

        WorkflowStepDecision decision = result.decision();
        if (decision == WorkflowStepDecision.WAIT_HUMAN) {
            log.info("Workflow completed — WAIT_HUMAN: chatId={}, runId={}", chatId, runId);
            sessionStore.completeRun(
                    chatId,
                    runId,
                    WorkflowTelegramRunStatus.WAITING_HUMAN,
                    summaryPath(result),
                    sanitizeForSession(result.message())
            );
            return;
        }

        if (decision == WorkflowStepDecision.STOP_FAILURE) {
            log.info("Workflow completed — STOP_FAILURE: chatId={}, runId={}", chatId, runId);
            sessionStore.failRun(
                    chatId,
                    runId,
                    sanitizeForSession(result.message() == null ? "Workflow failed" : result.message())
            );
            return;
        }

        log.info("Workflow completed — CONTINUE: chatId={}, runId={}", chatId, runId);
        sessionStore.completeRun(
                chatId,
                runId,
                WorkflowTelegramRunStatus.COMPLETED,
                summaryPath(result),
                sanitizeForSession(result.message())
        );
    }

    private Path summaryPath(WorkflowStepResult result) {
        Object value = result.data().get("workflowSummaryPath");
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        if (text.isBlank()) {
            return null;
        }
        return Path.of(text);
    }

    private String sanitizeForSession(RuntimeException e) {
        if (e == null) {
            return "Unknown error";
        }
        String raw = e.getMessage();
        if (raw == null || raw.isBlank()) {
            raw = e.toString();
        }
        return sanitizeForSession(raw);
    }

    private String sanitizeForSession(String raw) {
        WorkflowTelegramErrorSanitizer.SanitizedError sanitized = errorSanitizer.sanitizeForTelegram(raw);
        if (sanitized.rateLimit()) {
            return "Provider quota or rate limit reached. Wait and retry later, or switch provider/model.";
        }
        return sanitized.reason();
    }

    private String timeoutError(Throwable ex) {
        if (ex == null) {
            return "Workflow timed out";
        }
        Throwable cause = ex.getCause();
        if (cause != null && cause.getClass().getSimpleName().toLowerCase().contains("timeout")) {
            return "Workflow timed out";
        }
        String message = ex.getMessage();
        return (message == null || message.isBlank()) ? "Workflow timed out" : message.trim();
    }

    private String textValue(String value) {
        return value == null || value.isBlank() ? "(not set)" : value.trim();
    }

    private void executeAsync(Long chatId, String runId, Runnable runnable) {
        CompletableFuture
                .runAsync(runnable, executor)
                .orTimeout(timeout.toSeconds(), TimeUnit.SECONDS)
                .exceptionally(ex -> {
                    String raw = timeoutError(ex);
                    log.warn("Workflow async failed chatId={} runId={} error={}", chatId, runId, raw, ex);
                    sessionStore.failRun(chatId, runId, sanitizeForSession(raw));
                    return null;
                });
    }

    private java.util.Optional<AgentLlmConfig> resolveAgentLlmConfiguration(String telegramBotId) {
        if (agentDefinitionService == null || telegramBotId == null || telegramBotId.isBlank()) {
            return java.util.Optional.empty();
        }
        return agentDefinitionService.findByTelegramBotId(telegramBotId)
                .filter(def -> isPresent(def.llmProvider()) && isPresent(def.model()))
                .map(def -> new AgentLlmConfig(def.llmProvider().trim(), def.model().trim()));
    }

    private boolean isPresent(String value) {
        return value != null && !value.isBlank();
    }

    private record AgentLlmConfig(String llmProvider, String llmModel) {
    }

}
