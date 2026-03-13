package fr.ses10doigts.toolkitbridge.model.dto.web;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


public record FileContentResponse(
        boolean error,
        String message,
        String path,
        String content
) {
}