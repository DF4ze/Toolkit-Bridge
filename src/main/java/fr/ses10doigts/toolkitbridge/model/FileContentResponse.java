package fr.ses10doigts.toolkitbridge.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FileContentResponse {
    private boolean error;
    private String message;
    private String path;
    private String content;
}