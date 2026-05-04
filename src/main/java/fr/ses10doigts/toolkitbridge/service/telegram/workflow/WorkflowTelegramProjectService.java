package fr.ses10doigts.toolkitbridge.service.telegram.workflow;

import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.project.WorkflowProjectRegistrationResult;
import fr.ses10doigts.toolkitbridge.service.agent.workflow.runtime.project.WorkflowProjectRegistryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowTelegramProjectService {

    static final String TRY_COMMAND = "/workflow_project_set name=ToolkitBridge path=\"<project-path>\"";

    private final WorkflowProjectRegistryService registryService;

    public String setProject(List<String> args) {
        String name = argValue(args, "name");
        if (name == null) {
            log.warn("Workflow project set rejected — missing name");
            return WorkflowTelegramMessageRenderer.errorMessage("Missing project name", TRY_COMMAND);
        }

        ExtractedPath extractedPath = extractPath(args);
        if (!extractedPath.valid()) {
            log.warn("Workflow project set rejected — invalid path: name={}, reason={}", name, extractedPath.reason());
            return WorkflowTelegramMessageRenderer.errorMessage(extractedPath.reason(), TRY_COMMAND);
        }

        WorkflowProjectRegistrationResult result = registryService.registerOrUpdate(name, extractedPath.path());
        if (!result.success()) {
            log.warn("Workflow project set rejected — registry refused: name={}, reason={}", name, sanitizeReason(result.reason()));
            return WorkflowTelegramMessageRenderer.errorMessage(sanitizeReason(result.reason()), TRY_COMMAND);
        }

        log.info("Workflow project {}: name={}", result.created() ? "registered" : "updated", result.projectName());
        String verb = result.created() ? "registered" : "updated";
        return WorkflowTelegramMessageRenderer.successMessage(
                "Project " + verb + ": " + result.projectName(),
                "/workflow_run project=" + result.projectName() + " phase=... etape=..."
        );
    }

    String argValue(List<String> args, String key) {
        if (args == null || args.isEmpty() || key == null || key.isBlank()) {
            return null;
        }
        String prefix = key + "=";
        for (String arg : args) {
            if (arg == null) {
                continue;
            }
            String trimmed = arg.trim();
            if (trimmed.startsWith(prefix)) {
                String value = trimmed.substring(prefix.length()).trim();
                return value.isBlank() ? null : value;
            }
        }
        return null;
    }

    ExtractedPath extractPath(List<String> args) {
        if (args == null || args.isEmpty()) {
            return ExtractedPath.invalid("Missing project path");
        }

        int idx = findKeyIndex(args, "path");
        if (idx < 0) {
            return ExtractedPath.invalid("Missing project path");
        }

        String token = Objects.toString(args.get(idx), "").trim();
        String after = token.substring("path=".length()).trim();
        if (after.isBlank()) {
            return ExtractedPath.invalid("Missing project path");
        }

        if (startsWithQuote(after, '"')) {
            return collectQuoted(args, idx, after, '"');
        }
        if (startsWithQuote(after, '\'')) {
            return collectQuoted(args, idx, after, '\'');
        }

        // Not quoted: value must be in the same token.
        return ExtractedPath.valid(after);
    }

    private int findKeyIndex(List<String> args, String key) {
        String prefix = key + "=";
        for (int i = 0; i < args.size(); i++) {
            String a = args.get(i);
            if (a == null) {
                continue;
            }
            if (a.trim().startsWith(prefix)) {
                return i;
            }
        }
        return -1;
    }

    private boolean startsWithQuote(String value, char quote) {
        return value.length() >= 1 && value.charAt(0) == quote;
    }

    private ExtractedPath collectQuoted(List<String> args, int startIdx, String firstFragment, char quote) {
        StringBuilder sb = new StringBuilder();
        sb.append(firstFragment);
        if (endsWithQuote(firstFragment, quote) && firstFragment.length() >= 2) {
            return ExtractedPath.valid(stripEnclosingQuotes(sb.toString().trim(), quote));
        }

        for (int i = startIdx + 1; i < args.size(); i++) {
            String next = args.get(i);
            if (next == null) {
                continue;
            }
            sb.append(" ").append(next.trim());
            String current = sb.toString().trim();
            if (endsWithQuote(current, quote) && current.length() >= 2) {
                return ExtractedPath.valid(stripEnclosingQuotes(current, quote));
            }
        }

        return ExtractedPath.invalid("Invalid quoted path (missing closing quote)");
    }

    private boolean endsWithQuote(String value, char quote) {
        int len = value.length();
        return len >= 1 && value.charAt(len - 1) == quote;
    }

    private String stripEnclosingQuotes(String value, char quote) {
        String v = value.trim();
        if (v.length() >= 2 && v.charAt(0) == quote && v.charAt(v.length() - 1) == quote) {
            return v.substring(1, v.length() - 1);
        }
        return v;
    }

    private String sanitizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "Invalid project configuration";
        }
        String r = reason.trim();
        // Avoid leaking absolute paths if they ever appear in reasons.
        if (r.matches(".*[A-Za-z]:\\\\.*") || r.startsWith("/") || r.contains("/home/")) {
            return "Invalid project path";
        }
        if (r.length() > 200) {
            return r.substring(0, 200).trim();
        }
        return r;
    }

    record ExtractedPath(boolean valid, String path, String reason) {
        static ExtractedPath valid(String path) {
            return new ExtractedPath(true, path, null);
        }

        static ExtractedPath invalid(String reason) {
            return new ExtractedPath(false, null, reason);
        }
    }
}
