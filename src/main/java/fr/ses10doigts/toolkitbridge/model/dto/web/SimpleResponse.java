package fr.ses10doigts.toolkitbridge.model.dto.web;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimpleResponse {
    private boolean error;
    private String message;
}