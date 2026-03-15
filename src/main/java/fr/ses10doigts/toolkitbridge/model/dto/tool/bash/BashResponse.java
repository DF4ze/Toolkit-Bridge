package fr.ses10doigts.toolkitbridge.model.dto.tool.bash;


public record BashResponse (
        String stdout,
        String stderr,
        Integer exitCode,
        boolean timedOut
){}