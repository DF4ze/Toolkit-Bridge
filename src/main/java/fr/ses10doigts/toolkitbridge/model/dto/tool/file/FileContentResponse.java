package fr.ses10doigts.toolkitbridge.model.dto.tool.file;


public record FileContentResponse(
        String content,
        long   size
) {
}