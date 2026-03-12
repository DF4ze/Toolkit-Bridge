package fr.ses10doigts.toolkitbridge.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommandResponse {

    private boolean error;
    private String message;
    private String stdout;
    private String stderr;
    private Integer exitCode;
    private boolean timedOut;

}