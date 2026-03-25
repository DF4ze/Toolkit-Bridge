package fr.ses10doigts.toolkitbridge.service.workspace;

import java.util.Locale;

public final class WorkspacePathSanitizer {

    private WorkspacePathSanitizer() {
    }

    public static String sanitizeAgentFolderName(String agentIdent) {
        if (agentIdent == null || agentIdent.isBlank()) {
            throw new IllegalArgumentException("Agent identifier cannot be empty");
        }

        String normalized = agentIdent.trim().toLowerCase(Locale.ROOT);
        String sanitized = normalized.replaceAll("[^a-z0-9._-]", "_");

        if (sanitized.isBlank()) {
            throw new IllegalArgumentException("Agent identifier produces an empty workspace folder name");
        }

        return sanitized;
    }
}