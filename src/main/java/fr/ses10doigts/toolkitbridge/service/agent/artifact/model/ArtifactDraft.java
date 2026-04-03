package fr.ses10doigts.toolkitbridge.service.agent.artifact.model;

import java.util.Map;

public record ArtifactDraft(
        ArtifactType type,
        String taskId,
        String producerAgentId,
        String title,
        Map<String, String> metadata,
        String content,
        String mediaType,
        String fileName
) {
}
