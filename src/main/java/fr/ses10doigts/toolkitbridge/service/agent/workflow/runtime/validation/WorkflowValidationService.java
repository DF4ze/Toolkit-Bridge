package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.validation;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Executes a controlled local command and returns a structured validation result.
 *
 * <p>This class is intentionally not annotated with {@code @Service}. The Spring
 * annotation will be added when this service is wired into the workflow runtime
 * (a future step). Until then it can be instantiated directly.
 */
public class WorkflowValidationService {

    private static final int DEFAULT_TIMEOUT_SECONDS = 120;
    static final int MAX_OUTPUT_CHARS = 50_000;
    private static final String TRUNCATION_NOTICE =
            "\n[output truncated — " + MAX_OUTPUT_CHARS + " chars limit reached]";
    private static final int STREAM_COLLECTION_TIMEOUT_SECONDS = 5;

    public ValidationResult validate(ValidationCommand command) {
        if (command == null) {
            throw new IllegalArgumentException("command must not be null");
        }

        String commandString = String.join(" ", command.command());
        int timeoutSeconds = command.timeoutSeconds() != null
                ? command.timeoutSeconds()
                : DEFAULT_TIMEOUT_SECONDS;

        ProcessBuilder processBuilder = new ProcessBuilder(command.command());
        processBuilder.directory(command.workingDirectory().toFile());

        long startedAt = System.nanoTime();

        try {
            Process process = processBuilder.start();

            try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
                Future<String> stdoutFuture = executor.submit(() -> readStream(process.getInputStream()));
                Future<String> stderrFuture = executor.submit(() -> readStream(process.getErrorStream()));

                boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
                long durationMs = elapsedMs(startedAt);

                if (!finished) {
                    process.destroy();
                    try {
                        if (!process.waitFor(2, TimeUnit.SECONDS)) {
                            process.destroyForcibly();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        process.destroyForcibly();
                    }
                    return new ValidationResult(
                            commandString,
                            ValidationStatus.TIMEOUT,
                            null,
                            safeGet(stdoutFuture),
                            safeGet(stderrFuture),
                            durationMs,
                            "Process timed out after " + timeoutSeconds + " seconds"
                    );
                }

                int exitCode = process.exitValue();
                return new ValidationResult(
                        commandString,
                        exitCode == 0 ? ValidationStatus.SUCCESS : ValidationStatus.FAILURE,
                        exitCode,
                        safeGet(stdoutFuture),
                        safeGet(stderrFuture),
                        durationMs,
                        null
                );
            }
        } catch (IOException e) {
            return new ValidationResult(
                    commandString,
                    ValidationStatus.SYSTEM_ERROR,
                    null,
                    "",
                    "",
                    elapsedMs(startedAt),
                    "Failed to start process: " + e.getMessage()
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ValidationResult(
                    commandString,
                    ValidationStatus.SYSTEM_ERROR,
                    null,
                    "",
                    "",
                    elapsedMs(startedAt),
                    "Process execution interrupted"
            );
        }
    }

    String readStream(InputStream stream) throws IOException {
        StringBuilder builder = new StringBuilder();
        boolean truncated = false;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            char[] buffer = new char[8192];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                if (!truncated) {
                    int remaining = MAX_OUTPUT_CHARS - builder.length();
                    if (read <= remaining) {
                        builder.append(buffer, 0, read);
                    } else {
                        builder.append(buffer, 0, remaining);
                        truncated = true;
                    }
                }
                // Always drain the stream even after truncation to prevent process blocking
            }
        }
        if (truncated) {
            builder.append(TRUNCATION_NOTICE);
        }
        return builder.toString();
    }

    private String safeGet(Future<String> future) {
        try {
            return future.get(STREAM_COLLECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "";
        } catch (Exception e) {
            // TimeoutException or ExecutionException: stream lost, acceptable
            return "";
        }
    }

    private long elapsedMs(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000;
    }
}
