package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ValidationCommandTest {

    @TempDir
    Path tempDir;

    @Test
    void refusesNullCommand() {
        assertThatThrownBy(() -> new ValidationCommand(null, tempDir, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("command must not be null");
    }

    @Test
    void refusesEmptyCommand() {
        assertThatThrownBy(() -> new ValidationCommand(List.of(), tempDir, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("command must not be empty");
    }

    @Test
    void refusesBlankExecutable() {
        assertThatThrownBy(() -> new ValidationCommand(List.of("  "), tempDir, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("executable");
    }

    @Test
    void refusesNullExecutable() {
        List<String> commandWithNullFirst = new ArrayList<>();
        commandWithNullFirst.add(null);
        assertThatThrownBy(() -> new ValidationCommand(commandWithNullFirst, tempDir, null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refusesNullElementInCommand() {
        List<String> commandWithNullArg = new ArrayList<>();
        commandWithNullArg.add("echo");
        commandWithNullArg.add(null);
        assertThatThrownBy(() -> new ValidationCommand(commandWithNullArg, tempDir, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null");
    }

    @Test
    void refusesNullWorkingDirectory() {
        assertThatThrownBy(() -> new ValidationCommand(List.of("echo"), null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("workingDirectory must not be null");
    }

    @Test
    void refusesNonExistentWorkingDirectory() {
        Path nonExistent = tempDir.resolve("does_not_exist_xyz");
        assertThatThrownBy(() -> new ValidationCommand(List.of("echo"), nonExistent, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must exist");
    }

    @Test
    void refusesFileAsWorkingDirectory() throws IOException {
        Path file = Files.createTempFile(tempDir, "test", ".txt");
        assertThatThrownBy(() -> new ValidationCommand(List.of("echo"), file, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must be a directory");
    }

    @Test
    void refusesZeroTimeout() {
        assertThatThrownBy(() -> new ValidationCommand(List.of("echo"), tempDir, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timeoutSeconds must be greater than 0");
    }

    @Test
    void refusesNegativeTimeout() {
        assertThatThrownBy(() -> new ValidationCommand(List.of("echo"), tempDir, -1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timeoutSeconds must be greater than 0");
    }

    @Test
    void acceptsValidCommandWithoutTimeout() {
        ValidationCommand command = new ValidationCommand(List.of("echo", "ok"), tempDir, null);
        assertThat(command.command()).containsExactly("echo", "ok");
        assertThat(command.workingDirectory()).isEqualTo(tempDir);
        assertThat(command.timeoutSeconds()).isNull();
    }

    @Test
    void acceptsValidCommandWithTimeout() {
        ValidationCommand command = new ValidationCommand(List.of("echo"), tempDir, 30);
        assertThat(command.timeoutSeconds()).isEqualTo(30);
    }

    @Test
    void commandListIsImmutable() {
        List<String> mutable = new ArrayList<>(List.of("echo", "ok"));
        ValidationCommand command = new ValidationCommand(mutable, tempDir, null);
        mutable.add("extra");
        assertThat(command.command()).containsExactly("echo", "ok");
    }
}
