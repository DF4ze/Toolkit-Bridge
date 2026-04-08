package fr.ses10doigts.toolkitbridge.model.dto.web.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LlmAdminForm {

    @NotBlank(message = "LLM id is required.")
    @Size(max = 80, message = "LLM id must be at most 80 characters.")
    private String llmId;

    @NotBlank(message = "Base URL is required.")
    @Size(max = 512, message = "Base URL must be at most 512 characters.")
    private String baseUrl;

    @NotBlank(message = "Default model is required.")
    @Size(max = 120, message = "Default model must be at most 120 characters.")
    private String defaultModel;

    @Size(max = 1024, message = "API key must be at most 1024 characters.")
    private String apiKey;

    public String getLlmId() {
        return llmId;
    }

    public void setLlmId(String llmId) {
        this.llmId = llmId;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
}
