package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.codex;

public record CodexExecutionResult(
        String command,
        Integer exitCode,
        String stdout,
        String stderr,
        boolean success,
        boolean timedOut,
        Long durationMs
) {

    public CodexExecutionResult {
        if (command == null || command.isBlank()) {
            throw new IllegalArgumentException("command must not be blank");
        }
        if (durationMs != null && durationMs < 0) {
            throw new IllegalArgumentException("durationMs must be greater than or equal to 0");
        }

        command = command.trim();
        stdout = stdout == null ? "" : stdout;
        stderr = stderr == null ? "" : stderr;
    }
}
