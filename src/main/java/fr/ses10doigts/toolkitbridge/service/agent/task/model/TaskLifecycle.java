package fr.ses10doigts.toolkitbridge.service.agent.task.model;

import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

public final class TaskLifecycle {

    private static final Map<TaskStatus, Set<TaskStatus>> ALLOWED_TRANSITIONS = allowedTransitions();

    private TaskLifecycle() {
    }

    public static boolean canTransition(TaskStatus from, TaskStatus to) {
        if (from == null || to == null) {
            return false;
        }
        if (from == to) {
            return true;
        }
        Set<TaskStatus> allowedTargets = ALLOWED_TRANSITIONS.get(from);
        return allowedTargets != null && allowedTargets.contains(to);
    }

    public static void requireValidTransition(String taskId, TaskStatus from, TaskStatus to) {
        if (!canTransition(from, to)) {
            throw new InvalidTaskStatusTransitionException(taskId, from, to);
        }
    }

    public static boolean isTerminal(TaskStatus status) {
        return status == TaskStatus.DONE
                || status == TaskStatus.FAILED
                || status == TaskStatus.CANCELLED;
    }

    private static Map<TaskStatus, Set<TaskStatus>> allowedTransitions() {
        Map<TaskStatus, Set<TaskStatus>> transitions = new EnumMap<>(TaskStatus.class);
        transitions.put(TaskStatus.CREATED, Set.of(TaskStatus.RUNNING, TaskStatus.WAITING, TaskStatus.FAILED, TaskStatus.CANCELLED));
        transitions.put(TaskStatus.RUNNING, Set.of(TaskStatus.WAITING, TaskStatus.DONE, TaskStatus.FAILED, TaskStatus.CANCELLED));
        transitions.put(TaskStatus.WAITING, Set.of(TaskStatus.RUNNING, TaskStatus.FAILED, TaskStatus.CANCELLED));
        transitions.put(TaskStatus.DONE, Set.of());
        transitions.put(TaskStatus.FAILED, Set.of());
        transitions.put(TaskStatus.CANCELLED, Set.of());
        return Map.copyOf(transitions);
    }
}
