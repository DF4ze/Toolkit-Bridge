package fr.ses10doigts.toolkitbridge.service.admin;

import fr.ses10doigts.toolkitbridge.model.dto.admin.technical.TechnicalAdminView;
import fr.ses10doigts.toolkitbridge.service.admin.config.AdminTechnicalProperties;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.model.Artifact;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.service.ArtifactService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class ArtifactQueryService {

    private final ArtifactService artifactService;
    private final AdminTechnicalProperties technicalProperties;

    public ArtifactQueryService(ArtifactService artifactService, AdminTechnicalProperties technicalProperties) {
        this.artifactService = artifactService;
        this.technicalProperties = technicalProperties;
    }

    public List<TechnicalAdminView.ArtifactItem> listRecentArtifacts(Integer limit, String agentId, String taskId) {
        int effectiveLimit = technicalProperties.sanitizeLimit(limit);
        List<Artifact> artifacts;

        if (!isBlank(taskId)) {
            artifacts = artifactService.findByTaskId(taskId);
        } else if (!isBlank(agentId)) {
            artifacts = artifactService.findByProducerAgentId(agentId, effectiveLimit);
        } else {
            artifacts = artifactService.findRecent(effectiveLimit);
        }

        return artifacts.stream()
                .sorted(Comparator.comparing(Artifact::createdAt).reversed())
                .limit(effectiveLimit)
                .map(this::toArtifactItem)
                .toList();
    }

    private TechnicalAdminView.ArtifactItem toArtifactItem(Artifact artifact) {
        return new TechnicalAdminView.ArtifactItem(
                artifact.artifactId(),
                artifact.type() == null ? null : artifact.type().name(),
                artifact.taskId(),
                artifact.producerAgentId(),
                artifact.title(),
                artifact.createdAt(),
                artifact.expiresAt(),
                artifact.contentPointer() == null ? null : artifact.contentPointer().storageKind(),
                artifact.contentPointer() == null ? null : artifact.contentPointer().location(),
                artifact.contentPointer() == null ? null : artifact.contentPointer().mediaType(),
                artifact.contentPointer() == null ? 0 : artifact.contentPointer().sizeBytes()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}

