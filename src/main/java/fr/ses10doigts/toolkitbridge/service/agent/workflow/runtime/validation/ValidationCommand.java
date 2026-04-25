package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.validation;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Represents a controlled local command to execute for workflow validation.
 *
 * <p>The {@code command} must be provided as a list of individual arguments.
 * Passing a raw shell string is prohibited — this service does not use a shell
 * interpreter. The first element is the executable; subsequent elements are
 * its arguments.
 *
 * <p>This service is not a generic command executor. It is intended solely for
 * controlled, statically-defined build commands (e.g., Maven). No arbitrary
 * command from an external source should be passed to this record.
 */
public record ValidationCommand(
        List<String> command,
        Path workingDirectory,
        Integer timeoutSeconds
) {

    public ValidationCommand {
        if (command == null) {
            throw new IllegalArgumentException("command must not be null");
        }
        if (command.isEmpty()) {
            throw new IllegalArgumentException("command must not be empty");
        }
        if (command.get(0) == null || command.get(0).isBlank()) {
            throw new IllegalArgumentException("command executable (first element) must not be blank");
        }
        for (int i = 1; i < command.size(); i++) {
            if (command.get(i) == null) {
                throw new IllegalArgumentException("command element at index " + i + " must not be null");
            }
        }
        if (workingDirectory == null) {
            throw new IllegalArgumentException("workingDirectory must not be null");
        }
        if (!Files.exists(workingDirectory)) {
            throw new IllegalArgumentException("workingDirectory must exist: " + workingDirectory);
        }
        if (!Files.isDirectory(workingDirectory)) {
            throw new IllegalArgumentException("workingDirectory must be a directory: " + workingDirectory);
        }
        if (timeoutSeconds != null && timeoutSeconds <= 0) {
            throw new IllegalArgumentException("timeoutSeconds must be greater than 0");
        }
        command = List.copyOf(command);
    }
}
