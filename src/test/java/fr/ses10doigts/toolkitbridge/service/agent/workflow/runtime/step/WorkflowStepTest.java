package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.step;

import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowExecutionContext;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowRun;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model.WorkflowRunStatus;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowStepTest {

    @Test
    void executesSimpleDummyStep() {
        WorkflowRun workflowRun = new WorkflowRun(
                "run-1",
                "codex-implementation",
                "phase-1/step-2",
                WorkflowRunStatus.RUNNING,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
        WorkflowExecutionContext context = new WorkflowExecutionContext(workflowRun, null, Map.of());

        WorkflowStep step = executionContext -> new WorkflowStepResult(
                WorkflowStepDecision.CONTINUE,
                "ok",
                Map.of("runId", executionContext.workflowRun().runId())
        );

        WorkflowStepResult result = step.execute(context);

        assertThat(result.decision()).isEqualTo(WorkflowStepDecision.CONTINUE);
        assertThat(result.data()).containsEntry("runId", "run-1");
    }
}
