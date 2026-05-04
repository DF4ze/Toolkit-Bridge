package fr.ses10doigts.toolkitbridge.service.telegram.workflow;

import java.nio.file.Path;
import java.time.Instant;

final class WorkflowTelegramMessageRenderer {

    private WorkflowTelegramMessageRenderer() {
    }

    static String workflowHome(WorkflowTelegramSession session) {
        WorkflowTelegramSession safeSession = session == null ? WorkflowTelegramSession.idle(0L) : session;
        StringBuilder sb = new StringBuilder();
        sb.append("Workflow assistant").append("\n\n");
        sb.append("Current:").append("\n");
        sb.append("- Status: ").append(safeSession.lastStatus()).append("\n");
        sb.append("- Project: ").append(textOrFallback(safeSession.projectName(), "(not set)")).append("\n");
        sb.append("- Phase: ").append(numberOrFallback(safeSession.phase())).append("\n");
        sb.append("- Etape: ").append(numberOrFallback(safeSession.etape())).append("\n");
        sb.append("- Roadmap: ").append(roadmapState(safeSession.roadmapPath())).append("\n\n");
        sb.append("Commands:").append("\n");
        sb.append("- /workflow_run").append("\n");
        sb.append("- /workflow_status").append("\n");
        sb.append("- /workflow_summary").append("\n");
        sb.append("- /workflow_resume").append("\n");
        sb.append("- /workflow_project_set").append("\n");
        sb.append("- /workflow_roadmap_load").append("\n\n");
        sb.append("Next:").append("\n");
        sb.append("- ").append(recommendedNextAction(safeSession));
        return sb.toString();
    }

    static String statusView(WorkflowTelegramSession session) {
        WorkflowTelegramSession safeSession = session == null ? WorkflowTelegramSession.idle(0L) : session;
        StringBuilder sb = new StringBuilder();
        sb.append("Workflow status").append("\n\n");
        sb.append("Status: ").append(safeSession.lastStatus()).append("\n");
        sb.append("Project: ").append(textOrFallback(safeSession.projectName(), "(not set)")).append("\n");
        sb.append("Phase: ").append(numberOrFallback(safeSession.phase())).append("\n");
        sb.append("Etape: ").append(numberOrFallback(safeSession.etape())).append("\n");
        sb.append("Roadmap: ").append(roadmapState(safeSession.roadmapPath())).append("\n");
        sb.append("Summary: ").append(summaryState(safeSession.lastSummaryPath())).append("\n");
        sb.append("Run: ").append(textOrFallback(safeSession.lastRunId(), "(none)")).append("\n");
        sb.append("Started: ").append(formatInstant(safeSession.startedAt())).append("\n");
        sb.append("Updated: ").append(formatInstant(safeSession.updatedAt())).append("\n");
        if (safeSession.lastMessage() != null && !safeSession.lastMessage().isBlank()) {
            sb.append("Last message: ").append(safeSession.lastMessage().trim()).append("\n");
        }
        if (safeSession.lastError() != null && !safeSession.lastError().isBlank()) {
            sb.append("Last error: ").append(safeSession.lastError().trim()).append("\n");
        }
        sb.append("Next: ").append(recommendedNextAction(safeSession));
        return sb.toString();
    }

    static String successMessage(String detail, String nextAction) {
        StringBuilder sb = new StringBuilder();
        sb.append("Action effectuee").append("\n");
        if (detail != null && !detail.isBlank()) {
            sb.append("Detail: ").append(detail.trim()).append("\n");
        }
        sb.append("Next: ").append(textOrFallback(nextAction, "/workflow_status"));
        return sb.toString();
    }

    static String errorMessage(String reason, String tryCommand) {
        StringBuilder sb = new StringBuilder();
        sb.append("Action impossible").append("\n");
        sb.append("Reason: ").append(textOrFallback(reason, "Unknown error")).append("\n");
        sb.append("Try: ").append(textOrFallback(tryCommand, "/workflow_status"));
        return sb.toString();
    }

    static String recommendedNextAction(WorkflowTelegramSession session) {
        if (session == null) {
            return "/workflow_roadmap_load";
        }
        return switch (session.lastStatus()) {
            case IDLE -> session.roadmapPath() == null ? "/workflow_roadmap_load" : "/workflow_run";
            case RUNNING -> "/workflow_status";
            case WAITING_HUMAN -> "/workflow_summary puis /workflow_resume";
            case COMPLETED -> "/workflow_summary";
            case FAILED -> "/workflow_status puis inspecter l'erreur";
        };
    }

    private static String textOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private static String numberOrFallback(Integer value) {
        return value == null ? "(not set)" : Integer.toString(value);
    }

    private static String roadmapState(Path roadmapPath) {
        return roadmapPath == null ? "missing" : "loaded";
    }

    private static String summaryState(Path summaryPath) {
        return summaryPath == null ? "missing" : "available";
    }

    private static String formatInstant(Instant value) {
        return value == null ? "(not set)" : value.toString();
    }
}
