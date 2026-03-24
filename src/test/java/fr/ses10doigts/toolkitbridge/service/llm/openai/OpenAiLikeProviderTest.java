package fr.ses10doigts.toolkitbridge.service.llm.openai;

import fr.ses10doigts.toolkitbridge.config.llm.OpenAiLikeProperties;
import fr.ses10doigts.toolkitbridge.model.dto.llm.ChatRequest;
import fr.ses10doigts.toolkitbridge.model.dto.llm.ChatResponse;
import fr.ses10doigts.toolkitbridge.model.dto.llm.message.ChatMessage;
import fr.ses10doigts.toolkitbridge.model.dto.llm.message.MessageRole;
import fr.ses10doigts.toolkitbridge.service.llm.ProviderHttpExecutor;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OpenAiLikeProviderTest {

    private MockWebServer server;
    private OpenAiLikeMapper mapper;
    private ProviderHttpExecutor providerHttpExecutor;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        mapper = new OpenAiLikeMapper(new ObjectMapper());
        providerHttpExecutor = new ProviderHttpExecutor();
    }

    @AfterEach
    void tearDown() throws IOException {
        if (server != null) {
            server.shutdown();
        }
    }

    @Test
    void shouldUseRequestModelForChat() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                    {
                      "model": "qwen2.5",
                      "choices": [
                        {
                          "index": 0,
                          "message": {
                            "role": "assistant",
                            "content": "Bonjour",
                            "tool_calls": []
                          }
                        }
                      ]
                    }
                    """));

        OpenAiLikeProperties properties = new OpenAiLikeProperties(
                "lmstudio",
                server.url("/").toString(),
                null,
                "default-model"
        );

        OpenAiLikeProvider provider = new OpenAiLikeProvider(properties, mapper, providerHttpExecutor);

        ChatRequest request = new ChatRequest(
                "request-model",
                List.of(new ChatMessage(MessageRole.USER, "Salut", List.of())),
                List.of()
        );

        ChatResponse response = provider.chat(request);

        assertEquals("qwen2.5", response.model());
        assertEquals("Bonjour", response.message().getContent());

        RecordedRequest recorded = server.takeRequest();
        assertEquals("/chat/completions", recorded.getPath());
        assertEquals("POST", recorded.getMethod());
        assertTrue(recorded.getBody().readUtf8().contains("\"model\":\"request-model\""));
    }

    @Test
    void shouldUseDefaultModelWhenRequestModelIsBlank() throws Exception {
        server.enqueue(new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                    {
                      "model": "default-model",
                      "choices": [
                        {
                          "index": 0,
                          "message": {
                            "role": "assistant",
                            "content": "ok",
                            "tool_calls": []
                          }
                        }
                      ]
                    }
                    """));

        OpenAiLikeProvider provider = new OpenAiLikeProvider(
                new OpenAiLikeProperties("lmstudio", server.url("/").toString(), null, "default-model"),
                mapper,
                providerHttpExecutor
        );

        ChatRequest request = new ChatRequest(
                "   ",
                List.of(new ChatMessage(MessageRole.USER, "Hello", List.of())),
                List.of()
        );

        provider.chat(request);

        RecordedRequest recorded = server.takeRequest();
        assertTrue(recorded.getBody().readUtf8().contains("\"model\":\"default-model\""));
    }

    @Test
    void shouldFailWhenNoModelIsAvailable() {
        OpenAiLikeProvider provider = new OpenAiLikeProvider(
                new OpenAiLikeProperties("lmstudio", "http://localhost:1234", null, null),
                mapper,
                providerHttpExecutor
        );

        ChatRequest request = new ChatRequest(
                null,
                List.of(new ChatMessage(MessageRole.USER, "Hello", List.of())),
                List.of()
        );

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> provider.chat(request)
        );

        assertTrue(ex.getMessage().contains("No model specified"));
    }

    @Test
    void shouldListModels() {
        // même idée avec /models
    }
}
