package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LotRunTest {

    @Test
    void createsLotRunWithDefaultsAndDefensiveArtifacts() {
        List<String> references = new ArrayList<>();
        references.add("workspace/reports/lot-1-analysis.md");

        LotRun lotRun = new LotRun(
                " lot-1 ",
                " LOT-001 ",
                1,
                null,
                0,
                references,
                null,
                null,
                null,
                null,
                "  initial pass  "
        );

        references.add("workspace/reports/lot-1-review.md");

        assertThat(lotRun.lotId()).isEqualTo("lot-1");
        assertThat(lotRun.lotCode()).isEqualTo("LOT-001");
        assertThat(lotRun.status()).isEqualTo(LotRunStatus.PENDING);
        assertThat(lotRun.artifactReferences()).containsExactly("workspace/reports/lot-1-analysis.md");
        assertThat(lotRun.createdAt()).isNotNull();
        assertThat(lotRun.updatedAt()).isEqualTo(lotRun.createdAt());
        assertThat(lotRun.summary()).isEqualTo("initial pass");
        assertThatThrownBy(() -> lotRun.artifactReferences().add("other"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsNegativeCorrectionAttemptCount() {
        assertThatThrownBy(() -> new LotRun(
                "lot-1",
                "LOT-001",
                0,
                LotRunStatus.IN_PROGRESS,
                -1,
                List.of(),
                null,
                null,
                null,
                null,
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("correctionAttemptCount");
    }

    @Test
    void rejectsCompletedAtBeforeStartedAt() {
        Instant createdAt = Instant.parse("2026-04-20T10:00:00Z");
        Instant startedAt = Instant.parse("2026-04-20T11:00:00Z");
        Instant completedAt = Instant.parse("2026-04-20T10:30:00Z");

        assertThatThrownBy(() -> new LotRun(
                "lot-1",
                "LOT-001",
                0,
                LotRunStatus.IN_PROGRESS,
                0,
                List.of(),
                createdAt,
                createdAt,
                startedAt,
                completedAt,
                null
        )).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("completedAt");
    }
}
