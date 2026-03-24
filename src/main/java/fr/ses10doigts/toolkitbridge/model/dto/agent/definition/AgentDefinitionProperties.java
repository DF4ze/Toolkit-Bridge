package fr.ses10doigts.toolkitbridge.model.dto.agent.definition;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AgentDefinitionProperties {

    @NotBlank
    private String id;

    @NotBlank
    private String name;

    @NotBlank
    private String telegramBotId;

    @NotBlank
    private String orchestratorType;

    @NotBlank
    private String llmProvider;

    @NotBlank
    private String model;

    @NotBlank
    private String systemPrompt;
}