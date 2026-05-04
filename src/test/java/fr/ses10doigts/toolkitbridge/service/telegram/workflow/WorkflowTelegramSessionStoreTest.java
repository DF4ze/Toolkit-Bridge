package fr.ses10doigts.toolkitbridge.service.telegram.workflow;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowTelegramSessionStoreTest {

    @Test
    void getOrCreateCreatesIdleSession() {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();

        WorkflowTelegramSession session = store.getOrCreate(10L);

        assertThat(session.chatId()).isEqualTo(10L);
        assertThat(session.lastStatus()).isEqualTo(WorkflowTelegramRunStatus.IDLE);
        assertThat(session.running()).isFalse();
        assertThat(session.updatedAt()).isNotNull();
    }

    @Test
    void updateContextStoresProjectPhaseEtapeAndRoadmapPath() {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        Path roadmapPath = Path.of("workspace/shared/projects/Toolkit/roadmaps/phase7.md");

        WorkflowTelegramSession session = store.updateContext(
                42L,
                100L,
                "Toolkit",
                Path.of("D:/repo").toAbsolutePath().normalize(),
                7,
                2,
                roadmapPath
        );

        assertThat(session.chatId()).isEqualTo(42L);
        assertThat(session.userId()).isEqualTo(100L);
        assertThat(session.projectName()).isEqualTo("Toolkit");
        assertThat(session.projectPath()).isNotNull();
        assertThat(session.phase()).isEqualTo(7);
        assertThat(session.etape()).isEqualTo(2);
        assertThat(session.roadmapPath()).isEqualTo(roadmapPath);
    }

    @Test
    void tryMarkRunningMarksRunningAndRefusesSecondRun() {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();

        boolean started = store.tryMarkRunning(1L, "run-1");
        boolean secondStarted = store.tryMarkRunning(1L, "run-2");
        WorkflowTelegramSession session = store.getOrCreate(1L);

        assertThat(started).isTrue();
        assertThat(secondStarted).isFalse();
        assertThat(session.running()).isTrue();
        assertThat(session.lastRunId()).isEqualTo("run-1");
        assertThat(session.lastStatus()).isEqualTo(WorkflowTelegramRunStatus.RUNNING);
    }

    @Test
    void completeRunClearsRunningAndSetsStatusAndSummary() {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        store.tryMarkRunning(5L, "run-5");
        Path summaryPath = Path.of("data/rapport/v1.1/CodexTime/Phase7/workflow-summary.md");

        WorkflowTelegramSession session = store.completeRun(
                5L,
                "run-5",
                WorkflowTelegramRunStatus.COMPLETED,
                summaryPath,
                "done"
        );

        assertThat(session.running()).isFalse();
        assertThat(session.lastStatus()).isEqualTo(WorkflowTelegramRunStatus.COMPLETED);
        assertThat(session.lastSummaryPath()).isEqualTo(summaryPath);
        assertThat(session.lastMessage()).isEqualTo("done");
        assertThat(session.completedAt()).isNotNull();
    }

    @Test
    void failRunClearsRunningAndSetsFailed() {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        store.tryMarkRunning(6L, "run-6");

        WorkflowTelegramSession session = store.failRun(6L, "run-6", "boom");

        assertThat(session.running()).isFalse();
        assertThat(session.lastStatus()).isEqualTo(WorkflowTelegramRunStatus.FAILED);
        assertThat(session.lastError()).isEqualTo("boom");
        assertThat(session.completedAt()).isNotNull();
    }

    @Test
    void ignoresCompletionWhenRunIdDoesNotMatchOrWhenNotRunning() {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();
        store.tryMarkRunning(7L, "run-7");

        WorkflowTelegramSession failed = store.failRun(7L, "run-7", "timeout");
        assertThat(failed.lastStatus()).isEqualTo(WorkflowTelegramRunStatus.FAILED);

        WorkflowTelegramSession ignoredSameRun = store.completeRun(7L, "run-7", WorkflowTelegramRunStatus.COMPLETED, Path.of("x"), "done");
        assertThat(ignoredSameRun.lastStatus()).isEqualTo(WorkflowTelegramRunStatus.FAILED);

        WorkflowTelegramSession ignoredOtherRun = store.completeRun(7L, "run-OTHER", WorkflowTelegramRunStatus.COMPLETED, Path.of("y"), "done");
        assertThat(ignoredOtherRun.lastStatus()).isEqualTo(WorkflowTelegramRunStatus.FAILED);
    }

    @Test
    void updateContextReplacingProjectClearsProjectPathWhenNotProvided() {
        WorkflowTelegramSessionStore store = new WorkflowTelegramSessionStore();

        Path pathA = Path.of("D:/repoA").toAbsolutePath().normalize();
        WorkflowTelegramSession s1 = store.updateContext(1L, 1L, "ProjectA", pathA, 1, 1, null);
        assertThat(s1.projectName()).isEqualTo("ProjectA");
        assertThat(s1.projectPath()).isEqualTo(pathA);

        WorkflowTelegramSession s2 = store.updateContext(1L, 1L, "ProjectB", null, null, null, null);
        assertThat(s2.projectName()).isEqualTo("ProjectB");
        assertThat(s2.projectPath()).isNull();
    }
}
