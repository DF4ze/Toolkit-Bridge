package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.codex;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class CodexWorkflowClient {

    private static final String CODEX_BINARY = "C:\\Users\\Skill Korp\\AppData\\Roaming\\npm\\codex.cmd";
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(120);
    private static final int STREAM_COLLECTION_TIMEOUT_SECONDS = 5;

    public CodexExecutionResult execute(CodexExecutionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request must not be null");
        }

        List<String> command = buildCommand(request);
        log.debug("Codex CLI command built: command={}", String.join(" ", command));
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        if (request.workingDirectory() != null) {
            validateWorkingDirectory(request.workingDirectory());
            processBuilder.directory(request.workingDirectory().toFile());
        }

        long startedAt = System.nanoTime();
        try {
            log.info("Codex CLI process starting: workingDirectory={}",
                    request.workingDirectory() != null ? "set" : "none");
            Process process = processBuilder.start();
            writePromptToStdin(process, request.prompt());
            int timeoutSeconds = request.timeoutSeconds() == null
                    ? (int) DEFAULT_TIMEOUT.toSeconds()
                    : request.timeoutSeconds();

            try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
                Future<String> stdoutFuture = executor.submit(() -> readStream(process.getInputStream()));
                Future<String> stderrFuture = executor.submit(() -> readStream(process.getErrorStream()));

                boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
                long durationMs = elapsedMs(startedAt);

                if (!finished) {
                    log.warn("Codex CLI process timed out: durationMs={}", durationMs);
                    process.destroy();
                    if (!process.waitFor(2, TimeUnit.SECONDS)) {
                        process.destroyForcibly();
                    }
                    return new CodexExecutionResult(
                            String.join(" ", command),
                            null,
                            safeGet(stdoutFuture),
                            safeGet(stderrFuture),
                            false,
                            true,
                            durationMs
                    );
                }

                int exitCode = process.exitValue();
                String stdout = safeGet(stdoutFuture);
                String stderr = safeGet(stderrFuture);

                log.debug("Codex CLI execution finished: exitCode={}, durationMs={}, stdoutLen={}, stderrLen={}",
                        exitCode, durationMs, stdout.length(), stderr.length());

                return new CodexExecutionResult(
                        String.join(" ", command),
                        exitCode,
                        stdout,
                        stderr,
                        exitCode == 0,
                        false,
                        durationMs
                );
            }
        } catch (IOException e) {
            log.error("Failed to start Codex CLI process", e);
            throw new CodexExecutionException("Failed to start Codex CLI process", e);
        } catch (InterruptedException e) {
            log.error("Codex CLI execution interrupted", e);
            Thread.currentThread().interrupt();
            throw new CodexExecutionException("Codex CLI execution interrupted", e);
        }
    }

    List<String> buildCommand(CodexExecutionRequest request) {
        List<String> command = new ArrayList<>();
        command.add(CODEX_BINARY);
        command.add("exec");
        if (request.workingDirectory() != null) {
            command.add("--cd");
            command.add(request.workingDirectory().toString());
        }
        command.add("-");
        return List.copyOf(command);
    }

    private String readStream(InputStream stream) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            char[] buffer = new char[1024];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                builder.append(buffer, 0, read);
            }
        }
        return builder.toString();
    }

    private void writePromptToStdin(Process process, String prompt) {
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8))) {
            writer.write(prompt);
            writer.flush();
        } catch (IOException e) {
            log.error("Codex CLI failed to write prompt to stdin", e);
            process.destroy();
            throw new CodexExecutionException("Failed to write prompt to Codex CLI stdin", e);
        }
    }

    private String safeGet(Future<String> future) {
        try {
            return future.get(STREAM_COLLECTION_TIMEOUT_SECONDS, TimeUnit.SECONDS);
        } catch (Exception e) {
            return "";
        }
    }

    private void validateWorkingDirectory(Path workingDirectory) {
        if (!Files.exists(workingDirectory)) {
            throw new IllegalArgumentException("workingDirectory must exist");
        }
        if (!Files.isDirectory(workingDirectory)) {
            throw new IllegalArgumentException("workingDirectory must be a directory");
        }
    }

    private long elapsedMs(long startedAtNanos) {
        return (System.nanoTime() - startedAtNanos) / 1_000_000;
    }
}
