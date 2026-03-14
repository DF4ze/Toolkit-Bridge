package fr.ses10doigts.toolkitbridge.service.llm;

import fr.ses10doigts.toolkitbridge.config.OllamaProperties;
import fr.ses10doigts.toolkitbridge.model.dto.llm.OllamaChatRequest;
import fr.ses10doigts.toolkitbridge.model.dto.llm.OllamaChatResponse;
import fr.ses10doigts.toolkitbridge.model.dto.llm.OllamaMessage;
import fr.ses10doigts.toolkitbridge.model.dto.llm.OllamaToolDefinition;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
public class OllamaClientService implements OllamaClient {

    private final RestClient restClient;
    private final OllamaProperties properties;

    public OllamaClientService( OllamaProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.properties = properties;
    }

    @Override
    public OllamaChatResponse chat(List<OllamaMessage> messages, List<OllamaToolDefinition> tools) {
        OllamaChatRequest request = new OllamaChatRequest(
                properties.model(),
                messages,
                tools,
                false,
                "30m"
        );

        return restClient.post()
                .uri("/api/chat")
                .body(request)
                .retrieve()
                .body(OllamaChatResponse.class);
    }

    @Override
    public OllamaChatResponse chatWithToolResults(List<OllamaMessage> conversation, List<OllamaToolDefinition> tools) {
        return chat(conversation, tools);
    }
}
