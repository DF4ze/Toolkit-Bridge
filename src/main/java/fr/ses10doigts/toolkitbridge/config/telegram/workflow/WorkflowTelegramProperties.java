package fr.ses10doigts.toolkitbridge.config.telegram.workflow;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties(prefix = "toolkit.telegram.workflow")
public class WorkflowTelegramProperties {

    @NotBlank
    private String reportRootDirectory = "data/rapport";

    @NotBlank
    private String reportVersion = "v1.1";

    @Min(1)
    private int timeoutMinutes = 15;
}

