package fr.ses10doigts.toolkitbridge.service.llm.openai;

import fr.ses10doigts.toolkitbridge.config.llm.OpenAiLikeProperties;
import fr.ses10doigts.toolkitbridge.exception.LlmProviderException;
import fr.ses10doigts.toolkitbridge.model.dto.llm.ChatRequest;
import fr.ses10doigts.toolkitbridge.model.dto.llm.ChatResponse;
import fr.ses10doigts.toolkitbridge.model.dto.llm.provider.LlmCapability;
import fr.ses10doigts.toolkitbridge.model.dto.llm.provider.ModelInfo;
import fr.ses10doigts.toolkitbridge.model.dto.llm.provider.openai.OpenAiChatRequest;
import fr.ses10doigts.toolkitbridge.model.dto.llm.provider.openai.OpenAiChatResponse;
import fr.ses10doigts.toolkitbridge.model.dto.llm.provider.openai.OpenAiModelsResponse;
import fr.ses10doigts.toolkitbridge.service.llm.LlmProvider;
import fr.ses10doigts.toolkitbridge.service.llm.ProviderHttpExecutor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Set;

public class OpenAiLikeProvider implements LlmProvider {

    private static final String CHAT_COMPLETIONS_ENDPOINT = "/chat/completions";
    private static final String MODELS_ENDPOINT = "/models";

    private final OpenAiLikeProperties properties;
    private final RestClient restClient;
    private final OpenAiLikeMapper mapper;
    private final ProviderHttpExecutor httpExecutor;

    public OpenAiLikeProvider(OpenAiLikeProperties properties,
                              OpenAiLikeMapper mapper,
                              ProviderHttpExecutor httpExecutor) {
        this.properties = properties;
        this.mapper = mapper;
        this.httpExecutor = httpExecutor;
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeaders(headers -> {
                    if (properties.apiKey() != null && !properties.apiKey().isBlank()) {
                        headers.setBearerAuth(properties.apiKey());
                    }
                })
                .build();
    }

    @Override
    public String getName() {
        return properties.name();
    }

    @Override
    public Set<LlmCapability> getCapabilities() { // TODO retrieve from API
        return Set.of(
                LlmCapability.CHAT,
                LlmCapability.TOOL_CALLING,
                LlmCapability.MODELS_LISTING
        );
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        OpenAiChatRequest payload = mapper.toOpenAiChatRequest(resolveModel(request), request);

        OpenAiChatResponse response = httpExecutor.execute(
                getName(),
                CHAT_COMPLETIONS_ENDPOINT,
                () -> restClient.post()
                        .uri(CHAT_COMPLETIONS_ENDPOINT)
                        .body(payload)
                        .retrieve()
                        .body(OpenAiChatResponse.class)
        );

            if (response == null) {
                throw new LlmProviderException("Empty response from OpenAI-like provider");
            }

            return mapper.toChatResponse(response);

    }

    @Override
    public List<ModelInfo> listModels() {
        OpenAiModelsResponse response = httpExecutor.execute(
                getName(),
                MODELS_ENDPOINT,
                () -> restClient.get()
                        .uri(MODELS_ENDPOINT)
                        .retrieve()
                        .body(OpenAiModelsResponse.class)
        );

            if (response == null || response.data() == null) {
                return List.of();
            }

            return response.data().stream()
                    .map(model -> new ModelInfo(
                            model.id(),
                            model.id(),
                            getCapabilities()
                    ))
                    .toList();
    }

    private String resolveModel(ChatRequest request) {
        if (request.model() != null && !request.model().isBlank()) {
            return request.model();
        }

        if (properties.defaultModel() != null && !properties.defaultModel().isBlank()) {
            return properties.defaultModel();
        }

        throw new IllegalArgumentException("No model specified in request and no default model configured");
    }
}