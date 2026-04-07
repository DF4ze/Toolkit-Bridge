package fr.ses10doigts.toolkitbridge.service.agent.debate.model;

import java.util.Objects;
import java.util.UUID;

public record DebateContext(
        String debateId,
        String taskId,
        DebateStage stage,
        String rootMessageId,
        String parentMessageId,
        String subjectArtifactId,
        String initiatorAgentId
) {

    public DebateContext {
        if (debateId == null || debateId.isBlank()) {
            debateId = UUID.randomUUID().toString();
        }
        if (taskId == null || taskId.isBlank()) {
            throw new IllegalArgumentException("taskId must not be blank");
        }
        Objects.requireNonNull(stage, "stage must not be null");
        if (subjectArtifactId == null || subjectArtifactId.isBlank()) {
            throw new IllegalArgumentException("subjectArtifactId must not be blank");
        }
        if (initiatorAgentId == null || initiatorAgentId.isBlank()) {
            throw new IllegalArgumentException("initiatorAgentId must not be blank");
        }
        if (rootMessageId != null && rootMessageId.isBlank()) {
            throw new IllegalArgumentException("rootMessageId must not be blank when provided");
        }
        if (parentMessageId != null && parentMessageId.isBlank()) {
            throw new IllegalArgumentException("parentMessageId must not be blank when provided");
        }
    }

    public DebateContext withStage(DebateStage nextStage, String nextParentMessageId) {
        return new DebateContext(
                debateId,
                taskId,
                nextStage,
                rootMessageId,
                nextParentMessageId,
                subjectArtifactId,
                initiatorAgentId
        );
    }

    public DebateContext withRootMessage(String messageId) {
        return new DebateContext(
                debateId,
                taskId,
                stage,
                messageId,
                parentMessageId,
                subjectArtifactId,
                initiatorAgentId
        );
    }
}
