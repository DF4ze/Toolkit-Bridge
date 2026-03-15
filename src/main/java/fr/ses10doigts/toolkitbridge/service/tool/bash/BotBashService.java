package fr.ses10doigts.toolkitbridge.service.tool.bash;

import fr.ses10doigts.toolkitbridge.exception.ForbiddenCommandException;
import fr.ses10doigts.toolkitbridge.model.dto.tool.ToolExecutionResult;
import fr.ses10doigts.toolkitbridge.model.dto.web.CommandRequest;
import fr.ses10doigts.toolkitbridge.model.dto.tool.bash.BashResponse;
import fr.ses10doigts.toolkitbridge.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class BotBashService {

    private static final Logger log = LoggerFactory.getLogger(BotBashService.class);

    private static final int MAX_OUTPUT_CHARS = 20_000;
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private static final Map<String, String> ALLOWED_TOOLS = Map.of(
            "ls", "/bin/ls",
            "pwd", "/bin/pwd",
            "echo", "/bin/echo",
            "cat", "/bin/cat",
            "head", "/usr/bin/head",
            "tail", "/usr/bin/tail",
            "grep", "/usr/bin/grep",
            "find", "/usr/bin/find",
            "date", "/bin/date",
            "whoami", "/usr/bin/whoami"
    );

    private final WorkspaceService workspaceService;


    public ToolExecutionResult execute(CommandRequest request) throws Exception {
        validateRequest(request);

        String executable = ALLOWED_TOOLS.get(request.getTool());
        if (executable == null) {
            throw new ForbiddenCommandException("Tool not allowed: " + request.getTool());
        }

        List<String> command = new ArrayList<>();
        command.add(executable);

        List<String> args = request.getArgs() != null ? request.getArgs() : List.of();
        validateArgsForTool(request.getTool(), args);
        command.addAll(args);

        Path botWorkspace = workspaceService.getCurrentBotWorkspace();

        log.info(
                "Executing tool={} args={} workspace={}",
                request.getTool(),
                request.getArgs(),
                botWorkspace
        );

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(botWorkspace.toFile());

        Map<String, String> env = pb.environment();
        env.clear();
        env.put("LANG", "C");
        env.put("LC_ALL", "C");
        env.put("PATH", "/usr/bin:/bin");

        Process process = pb.start();

        ToolExecutionResult response;
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<String> stdoutFuture = executor.submit(() -> readStream(process.getInputStream()));
            Future<String> stderrFuture = executor.submit(() -> readStream(process.getErrorStream()));

            int timeoutSeconds = request.getTimeout() != null
                    ? request.getTimeout()
                    : (int) DEFAULT_TIMEOUT.toSeconds();

            try {
                boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

                if (!finished) {
                    process.destroy();

                    if (!process.waitFor(2, TimeUnit.SECONDS)) {
                        process.destroyForcibly();
                    }

                    response = ToolExecutionResult.builder()
                            .error(true)
                            .message("Timeout reached")
                            .bash(new BashResponse(
                                    safeGet(stdoutFuture),
                                    safeGet(stderrFuture),
                                    null,
                                    true
                                )
                            )
                            .build();


                } else {
                    int exitCode = process.exitValue();
                    String stdout = safeGet(stdoutFuture);
                    String stderr = safeGet(stderrFuture);

                    response = ToolExecutionResult.builder()
                            .error(exitCode != 0)
                            .message(exitCode == 0 ? "Command executed" : "Command failed")
                            .bash(new BashResponse(
                                    stdout.isBlank() ? "No output" : stdout,
                                    stderr,
                                    exitCode,
                                    false
                                )
                            )
                            .build();
                }
            } finally {
                executor.shutdownNow();
            }
        }

        return response;
    }

    private void validateRequest(CommandRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        if (request.getTool() == null || request.getTool().isBlank()) {
            throw new IllegalArgumentException("Tool cannot be empty");
        }

        if (request.getTimeout() != null && (request.getTimeout() < 1 || request.getTimeout() > 60)) {
            throw new IllegalArgumentException("Timeout must be between 1 and 60 seconds");
        }
    }

    private void validateArgsForTool(String tool, List<String> args) throws IOException {
        switch (tool) {
            case "pwd", "date", "whoami" -> validateNoArgs(tool, args);
            case "echo" -> validateEchoArgs(args);
            case "cat" -> validateFileOnlyArgs(tool, args);
            case "head", "tail" -> validateHeadTailArgs(tool, args);
            case "ls" -> validateLsArgs(args);
            case "find" -> validateFindArgs(args);
            case "grep" -> validateGrepArgs(args);
            default -> throw new ForbiddenCommandException("Tool not allowed: " + tool);
        }
    }

    private void validateNoArgs(String tool, List<String> args) {
        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Tool '" + tool + "' does not accept arguments");
        }
    }

    private void validateEchoArgs(List<String> args) {
        for (String arg : args) {
            workspaceService.validateCommandArg(arg);
        }
    }

    private void validateFileOnlyArgs(String tool, List<String> args) throws IOException {
        if (args.isEmpty()) {
            throw new IllegalArgumentException("Tool '" + tool + "' requires at least one file path");
        }

        for (String arg : args) {
            workspaceService.validateRelativeWorkspacePathArg(arg);
        }
    }

    private void validateHeadTailArgs(String tool, List<String> args) throws IOException {
        if (args.isEmpty()) {
            throw new IllegalArgumentException("Tool '" + tool + "' requires at least one argument");
        }

        for (String arg : args) {
            if (arg.startsWith("-")) {
                workspaceService.validateCommandArg(arg);
            } else {
                workspaceService.validateRelativeWorkspacePathArg(arg);
            }
        }
    }

    private void validateLsArgs(List<String> args) throws IOException {
        for (String arg : args) {
            if (arg.startsWith("-")) {
                workspaceService.validateCommandArg(arg);
            } else {
                workspaceService.validateRelativeWorkspacePathArg(arg);
            }
        }
    }

    private void validateFindArgs(List<String> args) throws IOException {
        if (args.isEmpty()) {
            workspaceService.validateRelativeWorkspacePathArg(".");
            return;
        }

        boolean pathConsumed = false;

        for (String arg : args) {
            if (!pathConsumed && !arg.startsWith("-")) {
                workspaceService.validateRelativeWorkspacePathArg(arg);
                pathConsumed = true;
            } else {
                workspaceService.validateCommandArg(arg);
            }
        }
    }

    private void validateGrepArgs(List<String> args) throws IOException {
        if (args.isEmpty()) {
            throw new IllegalArgumentException("Tool 'grep' requires at least a pattern");
        }

        boolean patternConsumed = false;

        for (String arg : args) {
            if (arg.startsWith("-")) {
                workspaceService.validateCommandArg(arg);
                continue;
            }

            if (!patternConsumed) {
                workspaceService.validateCommandArg(arg);
                patternConsumed = true;
                continue;
            }

            workspaceService.validateRelativeWorkspacePathArg(arg);
        }
    }

    private String readStream(InputStream inputStream) throws IOException {
        StringBuilder sb = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            char[] buffer = new char[1024];
            int read;
            while ((read = reader.read(buffer)) != -1) {
                int remaining = MAX_OUTPUT_CHARS - sb.length();
                if (remaining <= 0) {
                    break;
                }
                sb.append(buffer, 0, Math.min(read, remaining));
            }
        }

        if (sb.length() >= MAX_OUTPUT_CHARS) {
            sb.append("\n[output truncated]");
        }

        return sb.toString();
    }

    private String safeGet(Future<String> future) {
        try {
            return future.get(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            return "";
        }
    }
}