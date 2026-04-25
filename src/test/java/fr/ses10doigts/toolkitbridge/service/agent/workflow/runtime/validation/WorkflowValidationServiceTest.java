package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowValidationServiceTest {

    @TempDir
    Path tempDir;

    private final WorkflowValidationService service = new WorkflowValidationService();

    // --- OS detection helpers ---

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().contains("windows");
    }

    private static List<String> successCommand() {
        return isWindows()
                ? List.of("cmd", "/c", "echo", "ok")
                : List.of("echo", "ok");
    }

    private static List<String> failureCommand() {
        return isWindows()
                ? List.of("cmd", "/c", "dir", "nonexistentpath_xyz_987654")
                : List.of("false");
    }

    // Absolute path — stable on Windows Vista → 11; avoids PATH inheritance issues from the JVM process
    private static final String WINDOWS_POWERSHELL_PATH =
            "C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe";

    private static List<String> slowCommand() {
        // Uses a time-based sleep independent of stdin/network — reliable on both platforms
        return isWindows()
                ? List.of(WINDOWS_POWERSHELL_PATH,
                          "-NoProfile", "-NonInteractive", "-Command", "Start-Sleep -Seconds 30")
                : List.of("sleep", "30");
    }

    private ValidationCommand buildCommand(List<String> cmd) {
        return new ValidationCommand(cmd, tempDir, null);
    }

    private ValidationCommand buildCommand(List<String> cmd, int timeoutSeconds) {
        return new ValidationCommand(cmd, tempDir, timeoutSeconds);
    }

    // --- Tests: null guard ---

    @Test
    void throwsOnNullCommand() {
        assertThatThrownBy(() -> service.validate(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- Tests: SUCCESS ---

    @Test
    void returnsSuccessForSuccessfulCommand() {
        ValidationResult result = service.validate(buildCommand(successCommand()));

        assertThat(result.status()).isEqualTo(ValidationStatus.SUCCESS);
        assertThat(result.exitCode()).isEqualTo(0);
        assertThat(result.errorMessage()).isNull(); // contract: null when SUCCESS
    }

    @Test
    void durationIsPositiveForSuccessfulCommand() {
        ValidationResult result = service.validate(buildCommand(successCommand()));

        assertThat(result.durationMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    void commandStringIsIncludedInResult() {
        ValidationResult result = service.validate(buildCommand(successCommand()));

        assertThat(result.command()).isNotBlank();
    }

    // --- Tests: FAILURE ---

    @Test
    void returnsFailureForNonZeroExitCode() {
        ValidationResult result = service.validate(buildCommand(failureCommand()));

        assertThat(result.status()).isEqualTo(ValidationStatus.FAILURE);
        assertThat(result.exitCode()).isNotNull();
        assertThat(result.exitCode()).isNotEqualTo(0);
        assertThat(result.errorMessage()).isNull(); // contract: null when FAILURE
    }

    // --- Tests: SYSTEM_ERROR ---

    @Test
    void returnsSystemErrorForUnknownCommand() {
        List<String> unknownCommand = List.of("command_that_does_not_exist_xyz_12345");
        ValidationResult result = service.validate(buildCommand(unknownCommand));

        assertThat(result.status()).isEqualTo(ValidationStatus.SYSTEM_ERROR);
        assertThat(result.exitCode()).isNull();
        assertThat(result.errorMessage()).isNotBlank();
    }

    // --- Tests: TIMEOUT ---

    @Test
    void returnsTimeoutWhenProcessExceedsLimit() {
        ValidationResult result = service.validate(buildCommand(slowCommand(), 1));

        assertThat(result.status()).isEqualTo(ValidationStatus.TIMEOUT);
        assertThat(result.exitCode()).isNull();
        assertThat(result.errorMessage()).contains("timed out");
    }

    // --- Tests: stdout/stderr never null ---

    @Test
    void stdoutIsNeverNull() {
        ValidationResult success = service.validate(buildCommand(successCommand()));
        ValidationResult failure = service.validate(buildCommand(failureCommand()));
        ValidationResult error = service.validate(buildCommand(List.of("nonexistent_xyz")));

        assertThat(success.stdout()).isNotNull();
        assertThat(failure.stdout()).isNotNull();
        assertThat(error.stdout()).isNotNull();
    }

    @Test
    void stderrIsNeverNull() {
        ValidationResult success = service.validate(buildCommand(successCommand()));
        ValidationResult failure = service.validate(buildCommand(failureCommand()));
        ValidationResult error = service.validate(buildCommand(List.of("nonexistent_xyz")));

        assertThat(success.stderr()).isNotNull();
        assertThat(failure.stderr()).isNotNull();
        assertThat(error.stderr()).isNotNull();
    }

    // --- Tests: truncation (via package-private readStream) ---

    @Test
    void outputIsNotTruncatedWhenWithinLimit() throws IOException {
        String smallContent = "hello world";
        var stream = new ByteArrayInputStream(smallContent.getBytes(StandardCharsets.UTF_8));

        String result = service.readStream(stream);

        assertThat(result).isEqualTo(smallContent);
        assertThat(result).doesNotContain("[output truncated");
    }

    @Test
    void outputIsTruncatedWhenExceedingLimit() throws IOException {
        String largeContent = "x".repeat(WorkflowValidationService.MAX_OUTPUT_CHARS + 10_000);
        var stream = new ByteArrayInputStream(largeContent.getBytes(StandardCharsets.UTF_8));

        String result = service.readStream(stream);

        assertThat(result).contains("[output truncated");
        assertThat(result.length()).isLessThan(largeContent.length());
        assertThat(result.length()).isLessThanOrEqualTo(
                WorkflowValidationService.MAX_OUTPUT_CHARS + TRUNCATION_NOTICE_MAX_LENGTH
        );
    }

    @Test
    void outputAtExactLimitIsNotTruncated() throws IOException {
        String exactContent = "y".repeat(WorkflowValidationService.MAX_OUTPUT_CHARS);
        var stream = new ByteArrayInputStream(exactContent.getBytes(StandardCharsets.UTF_8));

        String result = service.readStream(stream);

        assertThat(result).doesNotContain("[output truncated");
        assertThat(result).hasSize(WorkflowValidationService.MAX_OUTPUT_CHARS);
    }

    // Max length of the truncation notice appended after the cap
    private static final int TRUNCATION_NOTICE_MAX_LENGTH = 100;
}
