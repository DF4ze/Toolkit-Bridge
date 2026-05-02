package fr.ses10doigts.toolkitbridge.service.telegram.workflow;

import fr.ses10doigts.telegrambots.model.TelegramUpdateContext;
import fr.ses10doigts.telegrambots.service.sender.TelegramSender;
import fr.ses10doigts.telegrambots.service.sender.TelegramSenderRegistry;
import fr.ses10doigts.toolkitbridge.config.telegram.workflow.WorkflowTelegramProperties;
import fr.ses10doigts.toolkitbridge.exception.ForbiddenCommandException;
import fr.ses10doigts.toolkitbridge.service.workspace.WorkspaceLayout;
import fr.ses10doigts.toolkitbridge.service.workspace.WorkspaceTextFileService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
public class WorkflowTelegramSummaryService {

    static final int MAX_TELEGRAM_TEXT = 3800;
    static final int MAX_CHUNKS = 2;

    private static final String NO_SUMMARY_REASON = "No summary available";
    private static final String SUMMARY_NOT_FOUND_REASON = "Summary not found on disk";
    private static final String SUMMARY_EMPTY_REASON = "Summary is empty";
    private static final String SUMMARY_TOO_LONG_SENT_REASON = "Summary too long. Complete file sent.";
    private static final String SUMMARY_TOO_LONG_NOT_SENT_REASON = "Summary too long. Complete file could not be sent.";
    private static final String SUMMARY_TOO_LONG_SENT_NEXT = "Open the attached document.";
    private static final String SUMMARY_TOO_LONG_NOT_SENT_TRY = "Check the summary file directly.";

    private final WorkflowTelegramSessionStore sessionStore;
    private final WorkspaceLayout workspaceLayout;
    private final WorkspaceTextFileService textFileService;
    private final Optional<TelegramSenderRegistry> senderRegistry;
    private final boolean telegramEnabled;
    private final Path reportRootDirectory;

    public WorkflowTelegramSummaryService(WorkflowTelegramSessionStore sessionStore,
                                          WorkspaceLayout workspaceLayout,
                                          WorkspaceTextFileService textFileService,
                                          Optional<TelegramSenderRegistry> senderRegistry,
                                          @Value("${telegram.enabled:true}") boolean telegramEnabled,
                                          WorkflowTelegramProperties properties) {
        this.sessionStore = sessionStore;
        this.workspaceLayout = workspaceLayout;
        this.textFileService = textFileService;
        this.senderRegistry = senderRegistry;
        this.telegramEnabled = telegramEnabled;
        this.reportRootDirectory = Path.of(properties == null ? "data/rapport" : properties.getReportRootDirectory())
                .toAbsolutePath()
                .normalize();
    }

    public String workflowSummary(TelegramUpdateContext ctx) {
        Long chatId = ctx == null ? null : ctx.getChatId();
        if (chatId == null) {
            return WorkflowTelegramMessageRenderer.errorMessage("Invalid request", "/workflow");
        }

        WorkflowTelegramSession session = sessionStore.getOrCreate(chatId);
        Path summaryPath = session.lastSummaryPath();
        if (summaryPath == null) {
            return WorkflowTelegramMessageRenderer.errorMessage(NO_SUMMARY_REASON, "/workflow_run");
        }

        ResolvedSummary resolved = resolveSummaryPath(summaryPath);
        if (!resolved.valid()) {
            return WorkflowTelegramMessageRenderer.errorMessage(SUMMARY_NOT_FOUND_REASON, "/workflow_status");
        }

        if (!textFileService.exists(resolved.absolutePath())) {
            return WorkflowTelegramMessageRenderer.errorMessage(SUMMARY_NOT_FOUND_REASON, "/workflow_status");
        }
        if (!textFileService.isRegularReadableFile(resolved.absolutePath())) {
            return WorkflowTelegramMessageRenderer.errorMessage(SUMMARY_NOT_FOUND_REASON, "/workflow_status");
        }

        String content;
        try {
            content = textFileService.readUtf8Text(resolved.absolutePath());
        } catch (IOException e) {
            return WorkflowTelegramMessageRenderer.errorMessage(SUMMARY_NOT_FOUND_REASON, "/workflow_status");
        }

        if (content == null || content.isBlank()) {
            return WorkflowTelegramMessageRenderer.errorMessage(SUMMARY_EMPTY_REASON, "/workflow_status");
        }

        SplitResult split = splitIntoBodies(content, MAX_TELEGRAM_TEXT, MAX_CHUNKS);
        if (split.tooLong()) {
            TelegramSender sender = resolveSender(ctx);
            if (sender == null) {
                return WorkflowTelegramMessageRenderer.errorMessage(SUMMARY_TOO_LONG_NOT_SENT_REASON, SUMMARY_TOO_LONG_NOT_SENT_TRY);
            }
            try {
                sender.sendDocument(chatId, resolved.absolutePath().toString(), "workflow-summary.md");
                return WorkflowTelegramMessageRenderer.successMessage(SUMMARY_TOO_LONG_SENT_REASON, SUMMARY_TOO_LONG_SENT_NEXT);
            } catch (RuntimeException e) {
                return WorkflowTelegramMessageRenderer.errorMessage(SUMMARY_TOO_LONG_NOT_SENT_REASON, SUMMARY_TOO_LONG_NOT_SENT_TRY);
            }
        }

        List<String> bodies = split.bodies();
        if (bodies.size() <= 1) {
            return formatSummaryMessage(1, 1, bodies.isEmpty() ? "" : bodies.getFirst());
        }

        if (bodies.size() == 2) {
            TelegramSender sender = resolveSender(ctx);
            if (sender != null) {
                sender.sendMessage(chatId, formatSummaryMessage(2, 2, bodies.get(1)));
                return formatSummaryMessage(1, 2, bodies.getFirst());
            }
            return formatSummaryMessage(1, 1, content);
        }

        return formatSummaryMessage(1, 1, content);
    }

    private ResolvedSummary resolveSummaryPath(Path summaryPath) {
        Objects.requireNonNull(summaryPath, "summaryPath must not be null");

        Path normalizedAbsolute = summaryPath.toAbsolutePath().normalize();
        String relativeText;
        try {
            relativeText = reportRootDirectory.relativize(normalizedAbsolute).toString();
        } catch (IllegalArgumentException e) {
            return ResolvedSummary.invalid();
        }

        try {
            Path resolved = workspaceLayout.resolveWithinRoot(reportRootDirectory, relativeText, "report root directory");
            return ResolvedSummary.valid(resolved);
        } catch (ForbiddenCommandException | IllegalArgumentException e) {
            return ResolvedSummary.invalid();
        }
    }

    private TelegramSender resolveSender(TelegramUpdateContext ctx) {
        if (!telegramEnabled || senderRegistry.isEmpty()) {
            return null;
        }
        TelegramSenderRegistry registry = senderRegistry.get();
        String botId = ctx == null ? null : ctx.getBotId();
        if (botId == null || botId.isBlank()) {
            return registry.getDefaultBotSender();
        }
        return registry.getRequiredSender(botId);
    }

    private SplitResult splitIntoBodies(String content, int maxTelegramText, int maxChunks) {
        if (content == null) {
            return SplitResult.valid(List.of(""));
        }

        int maxBodyLen = Math.max(200, maxTelegramText - 64);
        List<String> parts = splitByNewline(content, maxBodyLen);
        if (parts.size() <= 1) {
            return SplitResult.valid(parts);
        }

        if (parts.size() <= maxChunks) {
            return SplitResult.valid(parts);
        }
        return SplitResult.tooLongResult();
    }

    private List<String> splitByNewline(String content, int maxLen) {
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        int start = 0;
        while (start < content.length()) {
            int nextNewline = content.indexOf('\n', start);
            boolean lastLine = nextNewline < 0;
            int end = lastLine ? content.length() : nextNewline + 1;

            String line = content.substring(start, end);
            if (line.length() > maxLen) {
                flush(chunks, current);
                chunks.addAll(splitBrut(line, maxLen));
                start = end;
                continue;
            }

            if (current.length() + line.length() > maxLen) {
                flush(chunks, current);
            }
            current.append(line);
            start = end;
        }

        flush(chunks, current);
        if (chunks.isEmpty()) {
            return List.of("");
        }
        return chunks;
    }

    private List<String> splitBrut(String text, int maxLen) {
        if (text == null || text.isEmpty()) {
            return List.of("");
        }
        List<String> chunks = new ArrayList<>();
        int start = 0;
        while (start < text.length()) {
            int end = Math.min(text.length(), start + maxLen);
            chunks.add(text.substring(start, end));
            start = end;
        }
        return chunks;
    }

    private void flush(List<String> chunks, StringBuilder current) {
        if (current.length() <= 0) {
            return;
        }
        chunks.add(current.toString());
        current.setLength(0);
    }

    private String formatSummaryMessage(int index, int total, String body) {
        String safeBody = body == null ? "" : body;
        String header = "Workflow summary (" + index + "/" + total + ")\n";
        int maxBody = Math.max(0, MAX_TELEGRAM_TEXT - header.length());
        if (safeBody.length() > maxBody) {
            safeBody = safeBody.substring(0, maxBody);
        }
        return header + safeBody;
    }

    private record ResolvedSummary(boolean valid, Path absolutePath) {
        private static ResolvedSummary valid(Path absolutePath) {
            return new ResolvedSummary(true, absolutePath);
        }

        private static ResolvedSummary invalid() {
            return new ResolvedSummary(false, null);
        }
    }

    private static final class SplitResult {
        private final boolean tooLong;
        private final List<String> bodies;

        private SplitResult(boolean tooLong, List<String> bodies) {
            this.tooLong = tooLong;
            this.bodies = bodies == null ? List.of() : bodies;
        }

        private static SplitResult tooLongResult() {
            return new SplitResult(true, List.of());
        }

        private static SplitResult valid(List<String> bodies) {
            return new SplitResult(false, bodies);
        }

        private boolean tooLong() {
            return tooLong;
        }

        private List<String> bodies() {
            return bodies;
        }
    }
}
