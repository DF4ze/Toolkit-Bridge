package fr.ses10doigts.toolkitbridge.service.agent.task.model;

public class InvalidTaskStatusTransitionException extends IllegalStateException {

    public InvalidTaskStatusTransitionException(String taskId, TaskStatus from, TaskStatus to) {
        super("Invalid task status transition taskId=%s from=%s to=%s".formatted(taskId, from, to));
    }
}
