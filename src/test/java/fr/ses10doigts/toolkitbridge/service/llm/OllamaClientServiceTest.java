package fr.ses10doigts.toolkitbridge.service.llm;

import fr.ses10doigts.toolkitbridge.config.OllamaProperties;
import fr.ses10doigts.toolkitbridge.model.dto.llm.OllamaChatResponse;
import fr.ses10doigts.toolkitbridge.model.dto.llm.OllamaMessage;
import fr.ses10doigts.toolkitbridge.model.dto.llm.OllamaToolCall;
import fr.ses10doigts.toolkitbridge.model.dto.llm.OllamaToolDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.llm.OllamaToolSpec;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OllamaClientServiceTest {

    private OllamaClientService ollamaClientService;
    private MockRestServiceServer mockServer;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost:11434");
        mockServer = MockRestServiceServer.bindTo(builder).build();

        ollamaClientService = new OllamaClientService(
                builder.build(),
                new OllamaProperties("test://url.test", "qwen2.5-coder:7b", 60)
        );
    }

    @Test
    void chatShouldCallOllamaApiWithExpectedPayload() {
        mockServer.expect(requestTo("http://localhost:11434/api/chat"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"model\":\"qwen2.5-coder:7b\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("\"messages\"")))
                .andRespond(withSuccess(
                        """
                                {
                                  "model": "qwen2.5-coder:7b",
                                  "created_at": "2026-01-01T00:00:00Z",
                                  "done": true,
                                  "message": {
                                    "role": "assistant",
                                    "content": "Bonjour"
                                  }
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        OllamaChatResponse response = ollamaClientService.chat(
                List.of(new OllamaMessage("user", "Dis bonjour", null)),
                List.of(OllamaToolDefinition.function(new OllamaToolSpec(
                        "read_file",
                        "Read a file",
                        Map.of("type", "object")
                )))
        );

        assertNotNull(response);
        assertEquals("assistant", response.message().role());
        assertEquals("Bonjour", response.message().content());
        assertTrue(response.done());

        mockServer.verify();
    }

    @Test
    void chatWithToolResultsShouldReuseChatFlow() {
        mockServer.expect(requestTo("http://localhost:11434/api/chat"))
                .andExpect(method(HttpMethod.POST))
                .andRespond(withSuccess(
                        """
                                {
                                  "model": "qwen2.5-coder:7b",
                                  "created_at": "2026-01-01T00:00:00Z",
                                  "done": true,
                                  "message": {
                                    "role": "assistant",
                                    "content": "J'ai lu le fichier",
                                    "toolCalls": [
                                      {
                                        "function": {
                                          "name": "read_file",
                                          "arguments": {
                                            "path": "notes.txt"
                                          }
                                        }
                                      }
                                    ]
                                  }
                                }
                                """,
                        MediaType.APPLICATION_JSON
                ));

        OllamaChatResponse response = ollamaClientService.chatWithToolResults(
                List.of(new OllamaMessage("tool", "contenu du fichier", List.<OllamaToolCall>of())),
                List.of()
        );

        assertEquals("assistant", response.message().role());
        assertEquals("J'ai lu le fichier", response.message().content());
        mockServer.verify();
    }
}
