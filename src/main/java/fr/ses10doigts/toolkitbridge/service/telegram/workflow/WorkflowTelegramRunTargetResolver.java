package fr.ses10doigts.toolkitbridge.service.telegram.workflow;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
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
                return WorkflowTelegramRunTarget.error(
                        "Invalid target: projectName is required when phase and etape are provided",
                        WorkflowTelegramRunTargetErrorCode.MISSING_PROJECT
                );
            }
            return WorkflowTelegramRunTarget.ok(session.projectName(), phase, etape);
        }

        if (hasPhase && !hasEtape) {
            if (!hasProject && (session.projectName() == null || session.projectName().isBlank())) {
                return WorkflowTelegramRunTarget.error(
                        "Invalid target: projectName is required when phase is provided",
                        WorkflowTelegramRunTargetErrorCode.MISSING_PROJECT
                );
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
                return WorkflowTelegramRunTarget.error(
                        "No workflow context selected (missing projectName)",
                        WorkflowTelegramRunTargetErrorCode.MISSING_PROJECT
                );
            }
            if (session.phase() == null) {
                return WorkflowTelegramRunTarget.error(
                        "No workflow context selected (missing phase)",
                        WorkflowTelegramRunTargetErrorCode.MISSING_PHASE_OR_ETAPE
                );
            }
            if (session.etape() == null) {
                return WorkflowTelegramRunTarget.error(
                        "No workflow context selected (missing etape)",
                        WorkflowTelegramRunTargetErrorCode.MISSING_PHASE_OR_ETAPE
                );
            }
            return WorkflowTelegramRunTarget.ok(session.projectName(), session.phase(), session.etape());
        }

        // Partial / invalid combinations (keep simple and explicit).
        if (!hasPhase && hasEtape) {
            return WorkflowTelegramRunTarget.error(
                    "Invalid target: etape requires phase",
                    WorkflowTelegramRunTargetErrorCode.INVALID_PHASE_OR_ETAPE
            );
        }
        if (hasProject && !hasPhase && !hasEtape) {
            return WorkflowTelegramRunTarget.error(
                    "Invalid target: phase is required",
                    WorkflowTelegramRunTargetErrorCode.MISSING_PHASE_OR_ETAPE
            );
        }

        return WorkflowTelegramRunTarget.error("Invalid target parameters");
    }
}
