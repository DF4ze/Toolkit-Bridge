package fr.ses10doigts.toolkitbridge.service;

import fr.ses10doigts.toolkitbridge.exception.ForbiddenCommandException;
import fr.ses10doigts.toolkitbridge.model.CommandRequest;
import fr.ses10doigts.toolkitbridge.model.CommandResponse;
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
public class BashToolService {

    private static final Logger log = LoggerFactory.getLogger(BashToolService.class);

    private static final int MAX_OUTPUT_CHARS = 20_000;
    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

    /**
     * Workspace dédié au bot.
     * A adapter selon ton environnement.
     */
    private static final Path BOT_WORKDIR = Path.of("/opt/llm-tools/workdir").normalize();

    /**
     * Commandes shell éventuellement conservées.
     * Si tu veux supprimer totalement le shell, enlève toute la partie execute().
     */
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

    /**
     * Répertoire racine autorisé pour éviter de laisser l'outil balayer tout le système.
     * A adapter selon ton besoin.
     */
    private static final Path ALLOWED_BASE_DIR = Path.of("/home/oai/share").normalize();

    /**
     * Exécution shell limitée, facultative.
     */
    public CommandResponse execute(CommandRequest request) throws Exception {
        validateRequest(request);

        String executable = ALLOWED_TOOLS.get(request.getTool());
        if (executable == null) {
            throw new ForbiddenCommandException("Tool not allowed: " + request.getTool());
        }

        List<String> command = new ArrayList<>();
        command.add(executable);

        if (request.getArgs() != null) {
            for (String arg : request.getArgs()) {
                validateArg(arg);
                command.add(arg);
            }
        }

        log.info("Executing tool={} args={}", request.getTool(), request.getArgs());

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(BOT_WORKDIR.toFile());

        Map<String, String> env = pb.environment();
        env.clear();
        env.put("LANG", "C");
        env.put("LC_ALL", "C");
        env.put("PATH", "/usr/bin:/bin");

        Process process = pb.start();

        CommandResponse response;
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

                    response = new CommandResponse(
                            true,
                            "Timeout reached",
                            safeGet(stdoutFuture),
                            safeGet(stderrFuture),
                            null,
                            true
                    );
                } else {
                    int exitCode = process.exitValue();
                    String stdout = safeGet(stdoutFuture);
                    String stderr = safeGet(stderrFuture);

                    response = new CommandResponse(
                            exitCode != 0,
                            exitCode == 0 ? "Command executed" : "Command failed",
                            stdout.isBlank() ? "No output" : stdout,
                            stderr,
                            exitCode,
                            false
                    );
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

    private void validateArg(String arg) {
        if (arg == null) {
            throw new IllegalArgumentException("Argument cannot be null");
        }

        if (arg.length() > 300) {
            throw new IllegalArgumentException("Argument too long");
        }

        // Refus des caractères typiquement utilisés pour faire de l'injection shell
        // même si on n'utilise pas bash -c, ça évite des usages tordus.
        if (arg.matches(".*[\\r\\n\\x00].*")) {
            throw new IllegalArgumentException("Invalid control characters in argument");
        }

        // Si tu veux être plus strict, garde seulement un sous-ensemble de caractères.
        if (!arg.matches("[a-zA-Z0-9._/\\-:=,@+ ]*")) {
            throw new IllegalArgumentException("Forbidden characters in argument: " + arg);
        }

        // Vérification simple sur les chemins absolus/sortie de zone
        if (arg.startsWith("/")) {
            Path p = Path.of(arg).normalize();
            if (!p.startsWith(ALLOWED_BASE_DIR)) {
                throw new ForbiddenCommandException("Path outside allowed directory: " + arg);
            }
        }

        if (arg.contains("..")) {
            throw new ForbiddenCommandException("Parent path '..' is not allowed");
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