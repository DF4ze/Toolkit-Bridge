package fr.ses10doigts.toolkitbridge.service.agent.task.model;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TaskLifecycleTest {

    @Test
    void allowsNominalTransitions() {
        Task created = taskWithStatus(TaskStatus.CREATED);
        Task running = created.transitionTo(TaskStatus.RUNNING);
        Task waiting = running.transitionTo(TaskStatus.WAITING);
        Task resumed = waiting.transitionTo(TaskStatus.RUNNING);
        Task done = resumed.transitionTo(TaskStatus.DONE);

        assertThat(running.status()).isEqualTo(TaskStatus.RUNNING);
        assertThat(waiting.status()).isEqualTo(TaskStatus.WAITING);
        assertThat(done.status()).isEqualTo(TaskStatus.DONE);
        assertThat(TaskLifecycle.isTerminal(done.status())).isTrue();
    }

    @Test
    void rejectsInvalidTransition() {
        Task created = taskWithStatus(TaskStatus.CREATED);

        assertThatThrownBy(() -> created.transitionTo(TaskStatus.DONE))
                .isInstanceOf(InvalidTaskStatusTransitionException.class)
                .hasMessageContaining("from=CREATED")
                .hasMessageContaining("to=DONE");
    }

    @Test
    void keepsTerminalStatesClosed() {
        Task done = taskWithStatus(TaskStatus.DONE);
        Task failed = taskWithStatus(TaskStatus.FAILED);
        Task cancelled = taskWithStatus(TaskStatus.CANCELLED);

        assertThatThrownBy(() -> done.transitionTo(TaskStatus.RUNNING))
                .isInstanceOf(InvalidTaskStatusTransitionException.class);
        assertThatThrownBy(() -> failed.transitionTo(TaskStatus.RUNNING))
                .isInstanceOf(InvalidTaskStatusTransitionException.class);
        assertThatThrownBy(() -> cancelled.transitionTo(TaskStatus.RUNNING))
                .isInstanceOf(InvalidTaskStatusTransitionException.class);
    }

    private Task taskWithStatus(TaskStatus status) {
        return new Task(
                "task-lifecycle",
                "Execute objective",
                "user-1",
                "agent-1",
                null,
                "trace-1",
                TaskEntryPoint.TASK_ORCHESTRATOR,
                status,
                Map.of(),
                java.util.List.of()
        );
    }
}
