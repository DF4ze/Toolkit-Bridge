package fr.ses10doigts.toolkitbridge.service.agent.process.model;

public record ExternalProcessUpdateRequest(
        String processId,
        String description,
        String content,
        String mediaType,
        String updatedBy,
        String changeSummary
) {
}
