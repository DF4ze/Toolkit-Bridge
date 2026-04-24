package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.codex;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CodexExecutionRequestTest {

    @Test
    void createsRequestWithPromptAndOptionalFields() {
        CodexExecutionRequest request = new CodexExecutionRequest(
                " implement step ",
                Path.of("."),
                30
        );

        assertThat(request.prompt()).isEqualTo("implement step");
        assertThat(request.workingDirectory()).isEqualTo(Path.of("."));
        assertThat(request.timeoutSeconds()).isEqualTo(30);
    }

    @Test
    void rejectsBlankPrompt() {
        assertThatThrownBy(() -> new CodexExecutionRequest(" ", null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("prompt");
    }

    @Test
    void rejectsNonPositiveTimeoutWhenProvided() {
        assertThatThrownBy(() -> new CodexExecutionRequest("prompt", null, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timeoutSeconds");
    }
}
