package fr.ses10doigts.toolkitbridge.service.agent.debate.model;

import java.time.Instant;
import java.util.Objects;

public record DebateTranscriptEntry(
        String messageId,
        DebateStage stage,
        String agentId,
        String text,
        Instant timestamp,
        String parentMessageId
) {

    public DebateTranscriptEntry {
        if (messageId == null || messageId.isBlank()) {
            throw new IllegalArgumentException("messageId must not be blank");
        }
        Objects.requireNonNull(stage, "stage must not be null");
        if (agentId == null || agentId.isBlank()) {
            throw new IllegalArgumentException("agentId must not be blank");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        timestamp = timestamp == null ? Instant.now() : timestamp;
        if (parentMessageId != null && parentMessageId.isBlank()) {
            throw new IllegalArgumentException("parentMessageId must not be blank when provided");
        }
    }
}
