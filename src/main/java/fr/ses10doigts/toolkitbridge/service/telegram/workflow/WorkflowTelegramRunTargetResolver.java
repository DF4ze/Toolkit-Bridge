package fr.ses10doigts.toolkitbridge.service.telegram.workflow;

import org.springframework.stereotype.Service;

@Service
public class WorkflowTelegramRunTargetResolver {

    public WorkflowTelegramRunTarget resolveRunTarget(WorkflowTelegramSession session,
                                                     String projectName,
                                                     Integer phase,
                                                     Integer etape) {
        if (session == null) {
            return WorkflowTelegramRunTarget.error("Missing session");
        }

        boolean hasProject = projectName != null && !projectName.isBlank();
        boolean hasPhase = phase != null;
        boolean hasEtape = etape != null;

        if (hasProject && hasPhase && hasEtape) {
            return WorkflowTelegramRunTarget.ok(projectName.trim(), phase, etape);
        }

        if (!hasProject && hasPhase && hasEtape) {
            if (session.projectName() == null || session.projectName().isBlank()) {
                return WorkflowTelegramRunTarget.error("Invalid target: projectName is required when phase and etape are provided");
            }
            return WorkflowTelegramRunTarget.ok(session.projectName(), phase, etape);
        }

        if (hasPhase && !hasEtape) {
            if (!hasProject && (session.projectName() == null || session.projectName().isBlank())) {
                return WorkflowTelegramRunTarget.error("Invalid target: projectName is required when phase is provided");
            }
            if (session.phase() != null && session.phase().equals(phase) && session.etape() != null) {
                return WorkflowTelegramRunTarget.ok(
                        hasProject ? projectName.trim() : session.projectName(),
                        phase,
                        session.etape()
                );
            }
            return WorkflowTelegramRunTarget.ok(
                    hasProject ? projectName.trim() : session.projectName(),
                    phase,
                    1
            );
        }

        if (!hasProject && !hasPhase && !hasEtape) {
            if (session.projectName() == null || session.projectName().isBlank()) {
                return WorkflowTelegramRunTarget.error("No workflow context selected (missing projectName)");
            }
            if (session.phase() == null) {
                return WorkflowTelegramRunTarget.error("No workflow context selected (missing phase)");
            }
            if (session.etape() == null) {
                return WorkflowTelegramRunTarget.error("No workflow context selected (missing etape)");
            }
            return WorkflowTelegramRunTarget.ok(session.projectName(), session.phase(), session.etape());
        }

        // Partial / invalid combinations (keep simple and explicit).
        if (!hasPhase && hasEtape) {
            return WorkflowTelegramRunTarget.error("Invalid target: etape requires phase");
        }
        if (hasProject && !hasPhase && !hasEtape) {
            return WorkflowTelegramRunTarget.error("Invalid target: phase is required");
        }

        return WorkflowTelegramRunTarget.error("Invalid target parameters");
    }
}
