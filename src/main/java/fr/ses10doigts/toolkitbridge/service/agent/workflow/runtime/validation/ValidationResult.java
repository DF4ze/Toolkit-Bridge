package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.validation;

public record ValidationResult(
        String command,
        ValidationStatus status,
        Integer exitCode,
        String stdout,
        String stderr,
        long durationMs,
        /** null if status is SUCCESS or FAILURE; non-null for TIMEOUT or SYSTEM_ERROR. */
        String errorMessage
) {

    public ValidationResult {
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("command must not be blank");
        }
        if (status == null) {
            throw new IllegalArgumentException("status must not be null");
        }
        if (durationMs < 0) {
            throw new IllegalArgumentException("durationMs must be >= 0");
        }
        command = command.trim();
        stdout = stdout == null ? "" : stdout;
        stderr = stderr == null ? "" : stderr;
    }
}
