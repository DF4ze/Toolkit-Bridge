package fr.ses10doigts.toolkitbridge.model.dto.tool.file;


public record FileEntryResponse(
        String path,
        boolean directory,
        long size
) {
}