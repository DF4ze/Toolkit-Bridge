package fr.ses10doigts.toolkitbridge.service.telegram.workflow;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowTelegramRunTargetResolverTest {

    private final WorkflowTelegramRunTargetResolver resolver = new WorkflowTelegramRunTargetResolver();

    @Test
    void resolvesExplicitProjectPhaseEtape() {
        WorkflowTelegramSession session = WorkflowTelegramSession.idle(1L);

        WorkflowTelegramRunTarget target = resolver.resolveRunTarget(session, "Toolkit", 7, 3);

        assertThat(target.valid()).isTrue();
        assertThat(target.projectName()).isEqualTo("Toolkit");
        assertThat(target.phase()).isEqualTo(7);
        assertThat(target.etape()).isEqualTo(3);
    }

    @Test
    void resolvesPhaseEtapeWithoutProjectUsesSessionProjectWhenAvailable() {
        WorkflowTelegramSession session = new WorkflowTelegramSession(
                10L,
                20L,
                "Toolkit",
                7,
                4,
                null,
                null,
                WorkflowTelegramRunStatus.IDLE,
                false,
                null,
                null,
                java.time.Instant.now(),
                null,
                null,
                null
        );

        WorkflowTelegramRunTarget target = resolver.resolveRunTarget(session, null, 7, 3);

        assertThat(target.valid()).isTrue();
        assertThat(target.projectName()).isEqualTo("Toolkit");
        assertThat(target.phase()).isEqualTo(7);
        assertThat(target.etape()).isEqualTo(3);
    }

    @Test
    void resolvesPhaseOnlyUsesSessionEtapeWhenSamePhase() {
        WorkflowTelegramSession session = new WorkflowTelegramSession(
                10L,
                20L,
                "Toolkit",
                7,
                4,
                null,
                null,
                WorkflowTelegramRunStatus.IDLE,
                false,
                null,
                null,
                java.time.Instant.now(),
                null,
                null,
                null
        );

        WorkflowTelegramRunTarget target = resolver.resolveRunTarget(session, null, 7, null);

        assertThat(target.valid()).isTrue();
        assertThat(target.projectName()).isEqualTo("Toolkit");
        assertThat(target.phase()).isEqualTo(7);
        assertThat(target.etape()).isEqualTo(4);
    }

    @Test
    void resolvesPhaseOnlyDefaultsEtapeTo1WhenNoSessionEtape() {
        WorkflowTelegramSession session = new WorkflowTelegramSession(
                10L,
                null,
                "Toolkit",
                7,
                null,
                null,
                null,
                WorkflowTelegramRunStatus.IDLE,
                false,
                null,
                null,
                java.time.Instant.now(),
                null,
                null,
                null
        );

        WorkflowTelegramRunTarget target = resolver.resolveRunTarget(session, null, 7, null);

        assertThat(target.valid()).isTrue();
        assertThat(target.projectName()).isEqualTo("Toolkit");
        assertThat(target.phase()).isEqualTo(7);
        assertThat(target.etape()).isEqualTo(1);
    }

    @Test
    void resolvesNoParamsUsesSessionContextOrErrors() {
        WorkflowTelegramSession missing = WorkflowTelegramSession.idle(1L);

        WorkflowTelegramRunTarget error = resolver.resolveRunTarget(missing, null, null, null);

        assertThat(error.valid()).isFalse();
        assertThat(error.errorMessage()).contains("missing projectName");
    }
}
