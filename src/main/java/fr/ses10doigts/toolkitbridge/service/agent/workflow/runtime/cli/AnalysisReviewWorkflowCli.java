package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.cli;

import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.AnalysisReviewWorkflowRunner;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowExecutionContext;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowRun;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowRunStatus;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step.WorkflowStepResult;

import java.io.PrintStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class AnalysisReviewWorkflowCli {

    private static final String DEFAULT_WORKFLOW_TYPE = "analysis-review";
    private static final String DEFAULT_TARGET_STEP_REF = "external";
    private static final String ARG_MODE = "mode";
    private static final String VAR_REPORT_ROOT_DIRECTORY = "reportRootDirectory";
    private static final String VAR_REPORT_VERSION = "reportVersion";
    private static final String VAR_REPORT_PHASE = "reportPhase";
    private static final String VAR_STEP_NUMBER = "stepNumber";
    private static final String VAR_ANALYSIS_SOURCE_PATH = "analysisSourcePath";
    private static final String VAR_CODEX_WORKING_DIRECTORY = "codexWorkingDirectory";
    private static final String VAR_CODEX_TIMEOUT_SECONDS = "codexTimeoutSeconds";
    private static final String ARG_RUN_ID = "runId";
    private static final String ARG_WORKFLOW_TYPE = "workflowType";
    private static final String ARG_TARGET_STEP_REF = "targetStepRef";
    private static final String OUTPUT_FINAL_DECISION = "finalDecision";
    private static final String OUTPUT_NEXT_ACTION = "nextAction";
    private static final String OUTPUT_CORRECTION_TRIGGERED = "correctionTriggered";
    private static final String OUTPUT_WAIT_REASON = "waitReason";
    private static final String OUTPUT_WORKFLOW_SUMMARY_PATH = "workflowSummaryPath";

    private final AnalysisReviewWorkflowRunner runner;
    private final PrintStream out;
    private final PrintStream err;

    public AnalysisReviewWorkflowCli(AnalysisReviewWorkflowRunner runner, PrintStream out, PrintStream err) {
        this.runner = Objects.requireNonNull(runner, "runner must not be null");
        this.out = Objects.requireNonNull(out, "out must not be null");
        this.err = Objects.requireNonNull(err, "err must not be null");
    }

    public static void main(String[] args) {
        AnalysisReviewWorkflowCli cli = new AnalysisReviewWorkflowCli(
                AnalysisReviewWorkflowRunnerFactory.createDefault(),
                System.out,
                System.err
        );
        int exitCode = cli.execute(args);
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    public int execute(String[] args) {
        try {
            ParsedArguments parsedArguments = ParsedArguments.from(args);
            WorkflowExecutionContext context = buildContext(parsedArguments);
            WorkflowStepResult result = switch (parsedArguments.mode()) {
                case RUN -> runner.runAnalysisReviewWithOptionalCorrection(context);
                case RESUME -> runner.runCorrectionAfterReview(context);
            };
            printResult(result);
            return result.decision() == null ? 1 : 0;
        } catch (IllegalArgumentException e) {
            err.println(e.getMessage());
            printUsage(err);
            return 2;
        } catch (RuntimeException e) {
            err.println(e.getMessage());
            return 1;
        }
    }

    private WorkflowExecutionContext buildContext(ParsedArguments parsedArguments) {
        Map<String, Object> variables = new HashMap<>();
        variables.put(VAR_REPORT_ROOT_DIRECTORY, parsedArguments.requiredPath(VAR_REPORT_ROOT_DIRECTORY));
        variables.put(VAR_REPORT_VERSION, parsedArguments.required(VAR_REPORT_VERSION));
        variables.put(VAR_REPORT_PHASE, parsedArguments.required(VAR_REPORT_PHASE));
        variables.put(VAR_STEP_NUMBER, parsedArguments.requiredInt(VAR_STEP_NUMBER));

        if (parsedArguments.mode() == CliMode.RUN) {
            variables.put(VAR_ANALYSIS_SOURCE_PATH, parsedArguments.requiredPath(VAR_ANALYSIS_SOURCE_PATH));
        }
        parsedArguments.optionalPath(VAR_CODEX_WORKING_DIRECTORY)
                .ifPresent(path -> variables.put(VAR_CODEX_WORKING_DIRECTORY, path));
        parsedArguments.optionalInt(VAR_CODEX_TIMEOUT_SECONDS)
                .ifPresent(timeout -> variables.put(VAR_CODEX_TIMEOUT_SECONDS, timeout));

        Instant now = Instant.now();
        WorkflowRun workflowRun = new WorkflowRun(
                parsedArguments.valueOrDefault(ARG_RUN_ID, "workflow-" + now.toEpochMilli()),
                parsedArguments.valueOrDefault(ARG_WORKFLOW_TYPE, DEFAULT_WORKFLOW_TYPE),
                parsedArguments.valueOrDefault(ARG_TARGET_STEP_REF, DEFAULT_TARGET_STEP_REF),
                WorkflowRunStatus.RUNNING,
                null,
                null,
                now,
                now,
                now,
                null,
                Map.of()
        );
        return new WorkflowExecutionContext(workflowRun, null, variables);
    }

    private void printResult(WorkflowStepResult result) {
        Map<String, Object> data = result.data();
        out.println("decision=" + result.decision().name());
        out.println("message=" + safe(result.message()));
        out.println("finalDecision=" + safe(data.getOrDefault(OUTPUT_FINAL_DECISION, result.decision().name())));
        out.println("nextAction=" + safe(data.getOrDefault(OUTPUT_NEXT_ACTION, defaultNextAction(result))));
        out.println("correctionTriggered=" + safe(data.getOrDefault(OUTPUT_CORRECTION_TRIGGERED, false)));
        if (data.containsKey(OUTPUT_WAIT_REASON)) {
            out.println("waitReason=" + safe(data.get(OUTPUT_WAIT_REASON)));
        }
        if (data.containsKey(OUTPUT_WORKFLOW_SUMMARY_PATH)) {
            out.println("workflowSummaryPath=" + safe(data.get(OUTPUT_WORKFLOW_SUMMARY_PATH)));
        }
    }

    private static String safe(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString().replaceAll("\\R+", " ").trim();
    }

    private static String defaultNextAction(WorkflowStepResult result) {
        return switch (result.decision()) {
            case WAIT_HUMAN -> "Edit review result and resume with runCorrectionAfterReview(...)";
            case STOP_FAILURE -> "Inspect failure and related artifacts, then rerun";
            case CONTINUE -> "Continue workflow execution";
            default -> "Inspect workflow state";
        };
    }

    private static void printUsage(PrintStream stream) {
        stream.println("Usage: AnalysisReviewWorkflowCli --mode=RUN|RESUME --reportRootDirectory=<path> "
                + "--reportVersion=<version> --reportPhase=<phase> --stepNumber=<number> "
                + "[--analysisSourcePath=<path>] [--codexWorkingDirectory=<path>] [--codexTimeoutSeconds=<seconds>]");
    }

    private enum CliMode {
        RUN,
        RESUME
    }

    private record ParsedArguments(CliMode mode, Map<String, String> values) {

        private static ParsedArguments from(String[] args) {
            Map<String, String> values = new HashMap<>();
            if (args != null) {
                for (String arg : args) {
                    if (arg == null || arg.isBlank()) {
                        continue;
                    }
                    if (!arg.startsWith("--") || !arg.contains("=")) {
                        throw new IllegalArgumentException("Invalid argument: " + arg);
                    }
                    int separatorIndex = arg.indexOf('=');
                    String key = arg.substring(2, separatorIndex).trim();
                    String value = arg.substring(separatorIndex + 1).trim();
                    if (key.isBlank() || value.isBlank()) {
                        throw new IllegalArgumentException("Invalid argument: " + arg);
                    }
                    values.put(key, value);
                }
            }
            String modeValue = required(values, ARG_MODE).toUpperCase(Locale.ROOT);
            CliMode mode;
            try {
                mode = CliMode.valueOf(modeValue);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("mode must be RUN or RESUME");
            }
            return new ParsedArguments(mode, Map.copyOf(values));
        }

        private String required(String key) {
            return required(values, key);
        }

        private String valueOrDefault(String key, String fallback) {
            String value = values.get(key);
            return value == null || value.isBlank() ? fallback : value;
        }

        private Path requiredPath(String key) {
            return Path.of(required(key));
        }

        private int requiredInt(String key) {
            try {
                int value = Integer.parseInt(required(key));
                if (value <= 0) {
                    throw new IllegalArgumentException(key + " must be greater than 0");
                }
                return value;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(key + " must be a valid integer", e);
            }
        }

        private java.util.Optional<Path> optionalPath(String key) {
            String value = values.get(key);
            if (value == null || value.isBlank()) {
                return java.util.Optional.empty();
            }
            return java.util.Optional.of(Path.of(value));
        }

        private java.util.Optional<Integer> optionalInt(String key) {
            String value = values.get(key);
            if (value == null || value.isBlank()) {
                return java.util.Optional.empty();
            }
            try {
                int parsedValue = Integer.parseInt(value);
                if (parsedValue <= 0) {
                    throw new IllegalArgumentException(key + " must be greater than 0");
                }
                return java.util.Optional.of(parsedValue);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(key + " must be a valid integer", e);
            }
        }

        private static String required(Map<String, String> values, String key) {
            String value = values.get(key);
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Missing required argument: " + key);
            }
            return value;
        }
    }
}
