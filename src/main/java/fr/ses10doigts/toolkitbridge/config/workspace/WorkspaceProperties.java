package fr.ses10doigts.toolkitbridge.config.workspace;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "app.workspace")
public class WorkspaceProperties {

    @NotBlank
    private String agentsRoot;

    @NotBlank
    private String sharedRoot;

    @NotBlank
    private String globalContextRoot;

    @NotBlank
    private String scriptedToolsRoot = "workspace/shared/scripted-tools";

    @NotBlank
    private String externalProcessesRoot = "workspace/shared/processes";
}
