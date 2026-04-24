package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowExecutionContextTest {

    @Test
    void createsExecutionContextWithDefensiveVariables() {
        WorkflowRun workflowRun = new WorkflowRun(
                "run-1",
                "codex-implementation",
                "phase-1/step-4",
                WorkflowRunStatus.RUNNING,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        Map<String, Object> variables = new HashMap<>();
        variables.put("iteration", 1);

        WorkflowExecutionContext context = new WorkflowExecutionContext(workflowRun, null, variables);
        variables.put("iteration", 2);

        assertThat(context.variables()).containsEntry("iteration", 1);
        assertThatThrownBy(() -> context.variables().put("k", "v"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void requiresWorkflowRun() {
        assertThatThrownBy(() -> new WorkflowExecutionContext(null, null, Map.of()))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("workflowRun");
    }
}
