package fr.ses10doigts.toolkitbridge.model.dto.web.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class AgentDefinitionAdminForm {

    @NotBlank(message = "Agent id is required.")
    @Size(max = 100, message = "Agent id must be at most 100 characters.")
    private String agentId;

    @NotBlank(message = "Name is required.")
    @Size(max = 160, message = "Name must be at most 160 characters.")
    private String name;

    @NotBlank(message = "Telegram bot is required.")
    private String telegramBotId;

    @NotBlank(message = "Orchestrator type is required.")
    private String orchestratorType;

    @NotBlank(message = "LLM provider is required.")
    private String llmProvider;

    @NotBlank(message = "Model is required.")
    @Size(max = 200, message = "Model must be at most 200 characters.")
    private String model;

    @NotBlank(message = "System prompt is required.")
    private String systemPrompt;

    @NotBlank(message = "Role is required.")
    private String role;

    @NotBlank(message = "Policy name is required.")
    @Size(max = 120, message = "Policy name must be at most 120 characters.")
    private String policyName;

    private boolean toolsEnabled = true;

    public String getAgentId() {
        return agentId;
    }

    public void setAgentId(String agentId) {
        this.agentId = agentId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTelegramBotId() {
        return telegramBotId;
    }

    public void setTelegramBotId(String telegramBotId) {
        this.telegramBotId = telegramBotId;
    }

    public String getOrchestratorType() {
        return orchestratorType;
    }

    public void setOrchestratorType(String orchestratorType) {
        this.orchestratorType = orchestratorType;
    }

    public String getLlmProvider() {
        return llmProvider;
    }

    public void setLlmProvider(String llmProvider) {
        this.llmProvider = llmProvider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPolicyName() {
        return policyName;
    }

    public void setPolicyName(String policyName) {
        this.policyName = policyName;
    }

    public boolean isToolsEnabled() {
        return toolsEnabled;
    }

    public void setToolsEnabled(boolean toolsEnabled) {
        this.toolsEnabled = toolsEnabled;
    }
}
