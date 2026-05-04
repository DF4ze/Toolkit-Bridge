package fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.project;

import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Component
public class WorkflowProjectPathPolicy {

    private static final Set<String> FORBIDDEN_UNIX_ROOTS = Set.of(
            "/",
            "/BIN",
            "/BOOT",
            "/DEV",
            "/ETC",
            "/LIB",
            "/LIB64",
            "/PROC",
            "/ROOT",
            "/SBIN",
            "/SYS",
            "/USR",
            "/VAR"
    );

    private static final Set<String> FORBIDDEN_WINDOWS_FIRST_SEGMENTS = Set.of(
            "WINDOWS",
            "PROGRAM FILES",
            "PROGRAM FILES (X86)",
            "USERS"
    );

    public boolean isDangerous(Path normalizedAbsolutePath, PathOsFamily osFamily, Path userHome) {
        Objects.requireNonNull(normalizedAbsolutePath, "normalizedAbsolutePath must not be null");
        Objects.requireNonNull(osFamily, "osFamily must not be null");

        String raw = normalizedAbsolutePath.toString();
        if (raw.isBlank()) {
            return true;
        }

        return switch (osFamily) {
            case WINDOWS -> isDangerousWindows(raw, userHome == null ? null : userHome.toString());
            case UNIX -> isDangerousUnix(raw, userHome == null ? null : userHome.toString());
        };
    }

    private boolean isDangerousUnix(String raw, String userHomeRaw) {
        String normalized = normalizeUnix(raw);
        if (normalized.equals("/")) {
            return true;
        }
        for (String forbidden : FORBIDDEN_UNIX_ROOTS) {
            String f = forbidden.toLowerCase(Locale.ROOT);
            if (normalized.equals(f) || normalized.startsWith(f + "/")) {
                return true;
            }
        }
        if (userHomeRaw != null && !userHomeRaw.isBlank()) {
            String home = normalizeUnix(userHomeRaw);
            if (normalized.equals(home)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDangerousWindows(String raw, String userHomeRaw) {
        String normalized = normalizeWindows(raw);
        if (isDriveRoot(normalized)) {
            return true;
        }

        WindowsPathParts parts = splitWindows(normalized);
        if (!parts.valid()) {
            return true;
        }
        String first = parts.firstSegment();
        if (first != null && FORBIDDEN_WINDOWS_FIRST_SEGMENTS.contains(first)) {
            // Special-case Users:
            // - forbid drive:\\Users
            // - forbid home direct (drive:\\Users\\<user>)
            // - allow subfolders under home (drive:\\Users\\<user>\\...)
            if ("USERS".equals(first)) {
                if (parts.segmentsCount() <= 1) {
                    return true; // drive:\Users
                }
            } else {
                // Windows / Program Files / Program Files (x86): always forbid subtree.
                return true;
            }
        }

        if (userHomeRaw != null && !userHomeRaw.isBlank()) {
            String home = normalizeWindows(userHomeRaw);
            if (!home.isBlank() && normalized.equals(home)) {
                return true;
            }
        }
        return false;
    }

    private boolean isDriveRoot(String normalizedWindowsPath) {
        if (normalizedWindowsPath == null) {
            return false;
        }
        String p = normalizedWindowsPath;
        if (p.endsWith("\\")) {
            p = p.substring(0, p.length() - 1);
        }
        // "C:" or "C:\" after normalization => root.
        return p.length() == 2
                && isAsciiLetter(p.charAt(0))
                && p.charAt(1) == ':';
    }

    private boolean isAsciiLetter(char c) {
        return (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z');
    }

    private String normalizeWindows(String raw) {
        String s = raw.trim().replace('/', '\\');
        while (s.endsWith("\\") && s.length() > 3) {
            s = s.substring(0, s.length() - 1);
        }
        return s.toUpperCase(Locale.ROOT);
    }

    private WindowsPathParts splitWindows(String normalizedWindowsPath) {
        // Expected: "C:\FOO\BAR" after normalizeWindows(). We keep it defensive.
        int colonIdx = normalizedWindowsPath.indexOf(':');
        if (colonIdx != 1) {
            return WindowsPathParts.invalid();
        }
        if (normalizedWindowsPath.length() < 3 || normalizedWindowsPath.charAt(2) != '\\') {
            return WindowsPathParts.invalid();
        }
        String rest = normalizedWindowsPath.substring(3);
        if (rest.isEmpty()) {
            return WindowsPathParts.valid(null, 0);
        }
        int nextSlash = rest.indexOf('\\');
        if (nextSlash == -1) {
            return WindowsPathParts.valid(rest, 1);
        }
        String first = rest.substring(0, nextSlash);
        // Count segments quickly (first + remaining split count)
        int count = 1;
        for (int i = nextSlash + 1; i < rest.length(); i++) {
            if (rest.charAt(i) == '\\') {
                count++;
            }
        }
        // If there is content after last slash, it is another segment.
        if (rest.charAt(rest.length() - 1) != '\\') {
            count++;
        }
        return WindowsPathParts.valid(first, count);
    }

    private record WindowsPathParts(boolean valid, String firstSegment, int segmentsCount) {
        static WindowsPathParts valid(String firstSegment, int segmentsCount) {
            return new WindowsPathParts(true, firstSegment, segmentsCount);
        }

        static WindowsPathParts invalid() {
            return new WindowsPathParts(false, null, 0);
        }
    }

    private String normalizeUnix(String raw) {
        String s = raw.trim().replace('\\', '/');
        while (s.endsWith("/") && s.length() > 1) {
            s = s.substring(0, s.length() - 1);
        }
        return s.toLowerCase(Locale.ROOT);
    }
}
