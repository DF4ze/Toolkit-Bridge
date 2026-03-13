package fr.ses10doigts.toolkitbridge.model.dto.web;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


public record FileEntryResponse(
        String path,
        boolean directory,
        long size
) {
}