package fr.ses10doigts.toolkitbridge.service.telegram.workflow;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;

/**
 * Telegram workflow runtime state for one chat.
 *
 * <p>{@code phase + etape} represent the NEXT step to execute (not the last completed one).
 */
public record WorkflowTelegramSession(
        Long chatId,
        Long userId,
        String projectName,
        Path projectPath,
        Integer phase,
        Integer etape,
        Path roadmapPath,
        Path lastSummaryPath,
        WorkflowTelegramRunStatus lastStatus,
        boolean running,
        String lastRunId,
        Instant startedAt,
        Instant updatedAt,
        Instant completedAt,
        String lastMessage,
        String lastError
) {

    public WorkflowTelegramSession {
        if (chatId == null) {
            throw new IllegalArgumentException("chatId must not be null");
        }
        Objects.requireNonNull(lastStatus, "lastStatus must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }

    public static WorkflowTelegramSession idle(Long chatId) {
        Instant now = Instant.now();
        return new WorkflowTelegramSession(
                chatId,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                WorkflowTelegramRunStatus.IDLE,
                false,
                null,
                null,
                now,
                null,
                null,
                null
        );
    }
}
