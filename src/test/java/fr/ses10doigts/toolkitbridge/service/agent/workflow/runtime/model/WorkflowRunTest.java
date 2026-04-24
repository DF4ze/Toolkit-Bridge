package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowRunTest {

    @Test
    void createsWorkflowRunWithDefaultsAndDefensiveCollections() {
        LotRun lotRun = new LotRun(
                "lot-1",
                "LOT-001",
                0,
                LotRunStatus.PENDING,
                0,
                List.of(),
                null,
                null,
                null,
                null,
                null
        );
        List<LotRun> lots = new ArrayList<>(List.of(lotRun));
        Map<String, Object> context = new HashMap<>();
        context.put("workspace", "workspace/project-a");

        WorkflowRun workflowRun = new WorkflowRun(
                " run-1 ",
                " codex-implementation ",
                " phase-1/step-4 ",
                null,
                lots,
                " lot-1 ",
                null,
                null,
                null,
                null,
                context
        );

        lots.clear();
        context.put("workspace", "workspace/other");

        assertThat(workflowRun.runId()).isEqualTo("run-1");
        assertThat(workflowRun.workflowType()).isEqualTo("codex-implementation");
        assertThat(workflowRun.targetStepRef()).isEqualTo("phase-1/step-4");
        assertThat(workflowRun.status()).isEqualTo(WorkflowRunStatus.NEW);
        assertThat(workflowRun.currentLotId()).isEqualTo("lot-1");
        assertThat(workflowRun.lotRuns()).hasSize(1);
        assertThat(workflowRun.context()).containsEntry("workspace", "workspace/project-a");
        assertThat(workflowRun.updatedAt()).isEqualTo(workflowRun.createdAt());
        assertThatThrownBy(() -> workflowRun.context().put("k", "v"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsBlankRunId() {
        assertThatThrownBy(() -> new WorkflowRun(
                " ",
                "codex-implementation",
                "phase-1/step-4",
                WorkflowRunStatus.RUNNING,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                Map.of()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("runId");
    }

    @Test
    void rejectsUpdatedAtBeforeCreatedAt() {
        Instant createdAt = Instant.parse("2026-04-20T10:00:00Z");
        Instant updatedAt = Instant.parse("2026-04-20T09:00:00Z");

        assertThatThrownBy(() -> new WorkflowRun(
                "run-1",
                "codex-implementation",
                "phase-1/step-4",
                WorkflowRunStatus.RUNNING,
                List.of(),
                null,
                createdAt,
                updatedAt,
                null,
                null,
                Map.of()
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("updatedAt");
    }
}
