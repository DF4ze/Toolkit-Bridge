package fr.ses10doigts.toolkitbridge.service.agent.process.model;

import java.time.Instant;

public record ExternalProcessHistoryEntry(
        String processId,
        Instant changedAt,
        String changedBy,
        String changeSummary,
        String checksum,
        String backupPath
) {
}
