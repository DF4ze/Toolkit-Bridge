package fr.ses10doigts.toolkitbridge.service.agent.process.model;

import java.time.Instant;

public record ExternalProcessSnapshot(
        String processId,
        String description,
        String mediaType,
        String content,
        String checksum,
        String relativeContentPath,
        String relativeMetadataPath,
        Instant createdAt,
        Instant updatedAt,
        String updatedBy,
        String changeSummary
) {
}
