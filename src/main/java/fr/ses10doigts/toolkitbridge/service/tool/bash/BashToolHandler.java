package fr.ses10doigts.toolkitbridge.service.tool.bash;

import fr.ses10doigts.toolkitbridge.model.dto.tool.ToolExecutionResult;
import fr.ses10doigts.toolkitbridge.model.dto.tool.bash.BashRequest;
import fr.ses10doigts.toolkitbridge.model.dto.tool.bash.BashResponse;
import fr.ses10doigts.toolkitbridge.service.WorkspaceService;
import fr.ses10doigts.toolkitbridge.service.tool.JsonSchemaBuilder;
import fr.ses10doigts.toolkitbridge.service.tool.ToolHandler;
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
public class BashToolHandler implements ToolHandler {

    private static final Logger log = LoggerFactory.getLogger(BashToolHandler.class);

    private static final int MAX_OUTPUT_CHARS = 20_000;
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    private final WorkspaceService workspaceService;
    private final BashSecurityService bashSecurityService;


    @Override
    public String name() {
        return "run_command";
    }

    @Override
    public String description() {
        return "Run an allowed command in the workspace.";
    }

    @Override
    public Map<String, Object> parametersSchema() {
        return JsonSchemaBuilder.object()
                .enumStringProperty(
                        "command",
                        "Allowed command identifier",
                        bashSecurityService.getAllowedCommannds(),
                        true)
                .stringArrayProperty(
                        "args",
                        "Optional command arguments, each argument must be individually allowed",
                        false)
                .stringProperty(
                        "timeout",
                        "Timeout in seconds for the command execution (must be between 1 and 60)",
                        true
                )
                .build();
    }

    @Override
    public ToolExecutionResult execute(Map<String, Object> arguments) throws Exception {
        BashRequest br = new BashRequest();
        br.setTool((String)arguments.get("command"));
        br.setTimeout(Integer.valueOf((String)arguments.get("timeout")));

        if (arguments.get("args") instanceof List<?> list) {
            List<String> args = list.stream()
                    .map(Object::toString)
                    .toList();

            br.setArgs(args);
        }

        return execute(br);
    }

    public ToolExecutionResult execute(BashRequest request) throws Exception {
        bashSecurityService.validateRequest(request);

        List<String> args = request.getArgs() != null ? request.getArgs() : List.of();
        bashSecurityService.validateArgsForTool(request.getTool(), args);

        List<String> command = new ArrayList<>();
        command.add(bashSecurityService.getExecutable(request.getTool()));
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