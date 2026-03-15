package fr.ses10doigts.toolkitbridge.config.llm;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.ollama")
public record OllamaProperties(
        @NotBlank String baseUrl,
        @NotBlank String model,
        @Positive int timeoutSeconds
) {
}
