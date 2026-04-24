package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowStepResultTest {

    @Test
    void createsResultWithDefensiveData() {
        Map<String, Object> data = new HashMap<>();
        data.put("lotId", "lot-1");

        WorkflowStepResult result = new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                " step completed ",
                data
        );

        data.put("lotId", "lot-2");

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.CONTINUE);
        assertThat(result.message()).isEqualTo("step completed");
        assertThat(result.data()).containsEntry("lotId", "lot-1");
        assertThatThrownBy(() -> result.data().put("k", "v"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void requiresDecision() {
        assertThatThrownBy(() -> new WorkflowStepResult(
                null,
                "message",
                Map.of()
        )).isInstanceOf(NullPointerException.class)
                .hasMessageContaining("decision");
    }
}
