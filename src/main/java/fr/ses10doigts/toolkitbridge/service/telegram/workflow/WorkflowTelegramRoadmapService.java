package fr.ses10doigts.toolkitbridge.service.telegram.workflow;

import fr.ses10doigts.toolkitbridge.exception.ForbiddenCommandException;
import fr.ses10doigts.toolkitbridge.service.workspace.WorkspaceLayout;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

@Service
@Slf4j
public class WorkflowTelegramRoadmapService {

    static final String INVALID_REQUEST_MESSAGE = "Invalid request";
    static final String INVALID_ROADMAP_PATH_MESSAGE = "Invalid roadmap path";
    static final String ROADMAP_NOT_FOUND_MESSAGE = "Roadmap not found";
    static final String ROADMAP_WRONG_EXTENSION_MESSAGE = "Roadmap must be a .md file";
    static final String ROADMAP_EMPTY_MESSAGE = "Roadmap is empty";

    private final WorkspaceLayout workspaceLayout;
    private final WorkflowTelegramSessionStore sessionStore;

    public WorkflowTelegramRoadmapService(WorkspaceLayout workspaceLayout, WorkflowTelegramSessionStore sessionStore) {
        this.workspaceLayout = workspaceLayout;
        this.sessionStore = sessionStore;
    }

    public String loadRoadmap(Long chatId, Long userId, String projectName, String relativePath) {
        if (chatId == null) {
            return WorkflowTelegramMessageRenderer.errorMessage(INVALID_REQUEST_MESSAGE, "/workflow_roadmap_load path=<file.md>");
        }
        if (relativePath == null || relativePath.isBlank()) {
            log.warn("Roadmap rejected — missing path: chatId={}", chatId);
            return WorkflowTelegramMessageRenderer.errorMessage(INVALID_ROADMAP_PATH_MESSAGE, "/workflow_roadmap_load path=<file.md>");
        }

        Path sharedRoot;
        try {
            sharedRoot = workspaceLayout.sharedRoot();
        } catch (IOException e) {
            log.warn("Roadmap load failed — workspace root unavailable: chatId={}", chatId);
            return WorkflowTelegramMessageRenderer.errorMessage(INVALID_ROADMAP_PATH_MESSAGE, "/workflow_roadmap_load path=<file.md>");
        }

        Path resolved;
        try {
            resolved = workspaceLayout.resolveWithinRoot(sharedRoot, relativePath, "shared root");
        } catch (ForbiddenCommandException | IllegalArgumentException e) {
            log.warn("Roadmap rejected — forbidden path: chatId={}", chatId);
            return WorkflowTelegramMessageRenderer.errorMessage(INVALID_ROADMAP_PATH_MESSAGE+"\nInfo: "+e.getMessage(), "/workflow_roadmap_load path=<file.md>");
        }

        if (!Files.exists(resolved)) {
            log.warn("Roadmap not found: chatId={}", chatId);
            return WorkflowTelegramMessageRenderer.errorMessage(ROADMAP_NOT_FOUND_MESSAGE, "/workflow_roadmap_load path=<file.md>");
        }
        if (!Files.isRegularFile(resolved) || !Files.isReadable(resolved)) {
            log.warn("Roadmap rejected — not a readable file: chatId={}", chatId);
            return WorkflowTelegramMessageRenderer.errorMessage(INVALID_ROADMAP_PATH_MESSAGE, "/workflow_roadmap_load path=<file.md>");
        }

        String fileName = resolved.getFileName() == null ? "" : resolved.getFileName().toString();
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".md")) {
            log.warn("Roadmap rejected — invalid extension: chatId={}", chatId);
            return WorkflowTelegramMessageRenderer.errorMessage(ROADMAP_WRONG_EXTENSION_MESSAGE, "/workflow_roadmap_load path=<file.md>");
        }

        try {
            if (Files.size(resolved) <= 0 || isBlankTextFile(resolved)) {
                log.warn("Roadmap rejected — empty file: chatId={}", chatId);
                return WorkflowTelegramMessageRenderer.errorMessage(ROADMAP_EMPTY_MESSAGE, "/workflow_roadmap_load path=<file.md>");
            }
        } catch (IOException e) {
            log.warn("Roadmap rejected — read error: chatId={}", chatId);
            return WorkflowTelegramMessageRenderer.errorMessage(INVALID_ROADMAP_PATH_MESSAGE, "/workflow_roadmap_load path=<file.md>");
        }

        sessionStore.updateContext(chatId, userId, projectName, null, null, null, resolved);

        String normalizedProject = projectName == null || projectName.isBlank() ? "(not set)" : projectName.trim();
        String safeRelative = workspaceLayout.relativize(sharedRoot, resolved);
        String s = WorkflowTelegramMessageRenderer.successMessage(
                "Roadmap loaded for project=" + normalizedProject +
                        ", path=" + safeRelative,
                "/workflow_run"
        );

        log.info("Roadmap loaded: project={}, path={}", normalizedProject, safeRelative);

        return s;
    }

    private boolean isBlankTextFile(Path path) throws IOException {
        int remaining = 8192;
        boolean reachedEof = false;
        try (BufferedReader reader = Files.newBufferedReader(path)) {
            char[] buffer = new char[1024];
            while (remaining > 0) {
                int read = reader.read(buffer, 0, Math.min(buffer.length, remaining));
                if (read == -1) {
                    reachedEof = true;
                    break;
                }
                for (int i = 0; i < read; i++) {
                    if (!Character.isWhitespace(buffer[i])) {
                        return false;
                    }
                }
                remaining -= read;
            }
        }
        return reachedEof;
    }
}
