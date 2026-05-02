package fr.ses10doigts.toolkitbridge.service.telegram.workflow;

import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class WorkflowTelegramSessionStore {

    private final ConcurrentHashMap<Long, WorkflowTelegramSession> sessionsByChatId = new ConcurrentHashMap<>();

    public WorkflowTelegramSession getOrCreate(Long chatId) {
        Objects.requireNonNull(chatId, "chatId must not be null");
        return sessionsByChatId.computeIfAbsent(chatId, WorkflowTelegramSession::idle);
    }

    public WorkflowTelegramSession updateContext(Long chatId,
                                                 Long userId,
                                                 String projectName,
                                                 Integer phase,
                                                 Integer etape,
                                                 Path roadmapPath) {
        Objects.requireNonNull(chatId, "chatId must not be null");
        if (phase != null && phase <= 0) {
            throw new IllegalArgumentException("phase must be greater than 0");
        }
        if (etape != null && etape <= 0) {
            throw new IllegalArgumentException("etape must be greater than 0");
        }
        Instant now = Instant.now();

        return sessionsByChatId.compute(chatId, (key, existing) -> {
            WorkflowTelegramSession base = existing == null ? WorkflowTelegramSession.idle(chatId) : existing;
            return new WorkflowTelegramSession(
                    chatId,
                    userId != null ? userId : base.userId(),
                    projectName != null && !projectName.isBlank() ? projectName.trim() : base.projectName(),
                    phase != null ? phase : base.phase(),
                    etape != null ? etape : base.etape(),
                    roadmapPath != null ? roadmapPath : base.roadmapPath(),
                    base.lastSummaryPath(),
                    base.lastStatus(),
                    base.running(),
                    base.lastRunId(),
                    base.startedAt(),
                    now,
                    base.completedAt(),
                    base.lastMessage(),
                    base.lastError()
            );
        });
    }

    public boolean tryMarkRunning(Long chatId, String runId) {
        Objects.requireNonNull(chatId, "chatId must not be null");
        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("runId must not be blank");
        }
        Instant now = Instant.now();

        WorkflowTelegramSession updated = sessionsByChatId.compute(chatId, (key, existing) -> {
            WorkflowTelegramSession base = existing == null ? WorkflowTelegramSession.idle(chatId) : existing;
            if (base.running()) {
                return base;
            }
            return new WorkflowTelegramSession(
                    chatId,
                    base.userId(),
                    base.projectName(),
                    base.phase(),
                    base.etape(),
                    base.roadmapPath(),
                    base.lastSummaryPath(),
                    WorkflowTelegramRunStatus.RUNNING,
                    true,
                    runId.trim(),
                    now,
                    now,
                    null,
                    base.lastMessage(),
                    base.lastError()
            );
        });

        return updated.running() && runId.trim().equals(updated.lastRunId())
                && updated.lastStatus() == WorkflowTelegramRunStatus.RUNNING;
    }

    public WorkflowTelegramSession completeRun(Long chatId,
                                               String runId,
                                               WorkflowTelegramRunStatus status,
                                               Path lastSummaryPath,
                                               String lastMessage) {
        Objects.requireNonNull(chatId, "chatId must not be null");
        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("runId must not be blank");
        }
        Objects.requireNonNull(status, "status must not be null");
        if (status != WorkflowTelegramRunStatus.COMPLETED && status != WorkflowTelegramRunStatus.WAITING_HUMAN) {
            throw new IllegalArgumentException("status must be COMPLETED or WAITING_HUMAN");
        }
        Instant now = Instant.now();

        return sessionsByChatId.compute(chatId, (key, existing) -> {
            WorkflowTelegramSession base = existing == null ? WorkflowTelegramSession.idle(chatId) : existing;
            if (base.lastRunId() == null || !base.lastRunId().equals(runId.trim())) {
                return base;
            }
            if (!base.running() || base.lastStatus() != WorkflowTelegramRunStatus.RUNNING) {
                return base;
            }
            return new WorkflowTelegramSession(
                    chatId,
                    base.userId(),
                    base.projectName(),
                    base.phase(),
                    base.etape(),
                    base.roadmapPath(),
                    lastSummaryPath != null ? lastSummaryPath : base.lastSummaryPath(),
                    status,
                    false,
                    base.lastRunId(),
                    base.startedAt(),
                    now,
                    now,
                    lastMessage != null ? lastMessage : base.lastMessage(),
                    base.lastError()
            );
        });
    }

    public WorkflowTelegramSession failRun(Long chatId, String runId, String lastError) {
        Objects.requireNonNull(chatId, "chatId must not be null");
        if (runId == null || runId.isBlank()) {
            throw new IllegalArgumentException("runId must not be blank");
        }
        if (lastError == null || lastError.isBlank()) {
            throw new IllegalArgumentException("lastError must not be blank");
        }
        Instant now = Instant.now();

        return sessionsByChatId.compute(chatId, (key, existing) -> {
            WorkflowTelegramSession base = existing == null ? WorkflowTelegramSession.idle(chatId) : existing;
            if (base.lastRunId() == null || !base.lastRunId().equals(runId.trim())) {
                return base;
            }
            if (!base.running() || base.lastStatus() != WorkflowTelegramRunStatus.RUNNING) {
                return base;
            }
            return new WorkflowTelegramSession(
                    chatId,
                    base.userId(),
                    base.projectName(),
                    base.phase(),
                    base.etape(),
                    base.roadmapPath(),
                    base.lastSummaryPath(),
                    WorkflowTelegramRunStatus.FAILED,
                    false,
                    base.lastRunId(),
                    base.startedAt(),
                    now,
                    now,
                    base.lastMessage(),
                    lastError.trim()
            );
        });
    }
}
