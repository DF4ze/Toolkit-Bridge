package fr.ses10doigts.toolkitbridge.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileEntryResponse {
    private String path;
    private boolean directory;
    private long size;
}