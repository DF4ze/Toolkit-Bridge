package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.codex;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodexExecutionResultTest {

    @Test
    void createsResultWithNormalizedFields() {
        CodexExecutionResult result = new CodexExecutionResult(
                " codex prompt ",
                0,
                null,
                null,
                true,
                false,
                25L
        );

        assertThat(result.command()).isEqualTo("codex prompt");
        assertThat(result.exitCode()).isZero();
        assertThat(result.stdout()).isEmpty();
        assertThat(result.stderr()).isEmpty();
        assertThat(result.success()).isTrue();
        assertThat(result.timedOut()).isFalse();
        assertThat(result.durationMs()).isEqualTo(25L);
    }

    @Test
    void rejectsBlankCommand() {
        assertThatThrownBy(() -> new CodexExecutionResult(
                " ",
                1,
                "",
                "",
                false,
                false,
                1L
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("command");
    }

    @Test
    void rejectsNegativeDuration() {
        assertThatThrownBy(() -> new CodexExecutionResult(
                "codex prompt",
                1,
                "",
                "",
                false,
                false,
                -1L
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("durationMs");
    }
}
