package fr.ses10doigts.toolkitbridge.service.llm;

import fr.ses10doigts.toolkitbridge.model.dto.llm.ChatRequest;
import fr.ses10doigts.toolkitbridge.model.dto.llm.ChatResponse;
import fr.ses10doigts.toolkitbridge.model.dto.llm.message.ChatMessage;
import fr.ses10doigts.toolkitbridge.model.dto.llm.message.Message;
import fr.ses10doigts.toolkitbridge.model.dto.llm.message.MessageRole;
import fr.ses10doigts.toolkitbridge.model.dto.llm.tooling.ToolCall;
import fr.ses10doigts.toolkitbridge.model.dto.llm.tooling.ToolDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.llm.tooling.ToolFunction;
import fr.ses10doigts.toolkitbridge.model.dto.llm.tooling.ToolSpec;
import fr.ses10doigts.toolkitbridge.model.dto.tool.ToolExecutionResult;
import fr.ses10doigts.toolkitbridge.service.agent.trace.AgentTraceContextHolder;
import fr.ses10doigts.toolkitbridge.service.agent.trace.AgentTraceService;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceCorrelation;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEventType;
import fr.ses10doigts.toolkitbridge.service.llm.provider.LlmProvider;
import fr.ses10doigts.toolkitbridge.service.llm.provider.LlmProviderRegistry;
import fr.ses10doigts.toolkitbridge.service.llm.runtime.LlmProviderRegistryRuntime;
import fr.ses10doigts.toolkitbridge.service.tool.ToolExecutionService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DefaultLlmServiceTest {

    @Test
    void emitsStructuredToolCallTraceWhenToolExecutionSucceeds() throws Exception {
        LlmProviderRegistryRuntime registryRuntime = mock(LlmProviderRegistryRuntime.class);
        LlmProviderRegistry providerRegistry = mock(LlmProviderRegistry.class);
        ToolExecutionService toolExecutionService = mock(ToolExecutionService.class);
        AgentTraceService traceService = mock(AgentTraceService.class);
        LlmProvider provider = mock(LlmProvider.class);

        AgentTraceContextHolder contextHolder = new AgentTraceContextHolder();
        contextHolder.setCurrentCorrelation(new AgentTraceCorrelation("run-1", "task-1", "agent-1", "message-1"));
        DefaultLlmService service = new DefaultLlmService(
                registryRuntime,
                toolExecutionService,
                new ObjectMapper(),
                traceService,
                contextHolder
        );

        ToolCall toolCall = new ToolCall("call-1", new ToolFunction("read_file", Map.of("path", "README.md")));
        ChatMessage toolMessage = new ChatMessage(MessageRole.ASSISTANT, "", List.of(toolCall));
        ChatMessage finalMessage = new ChatMessage(MessageRole.ASSISTANT, "done", null);

        when(registryRuntime.snapshot()).thenReturn(providerRegistry);
        when(providerRegistry.getRequired("provider")).thenReturn(provider);
        when(provider.chat(any(ChatRequest.class)))
                .thenReturn(new ChatResponse("model", toolMessage))
                .thenReturn(new ChatResponse("model", finalMessage));
        when(toolExecutionService.execute(toolCall))
                .thenReturn(ToolExecutionResult.builder().error(false).message("ok").build());

        String response = service.chat(
                "provider",
                "model",
                "system",
                "user",
                List.of(ToolDefinition.function(new ToolSpec("read_file", "Read a file", Map.of())))
        );

        assertThat(response).isEqualTo("done");

        ArgumentCaptor<Map<String, Object>> attributesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(traceService).trace(
                eq(AgentTraceEventType.TOOL_CALL),
                eq("llm_service"),
                any(AgentTraceCorrelation.class),
                attributesCaptor.capture()
        );
        assertThat(attributesCaptor.getValue()).containsEntry("toolName", "read_file");
        assertThat(attributesCaptor.getValue()).containsEntry("success", true);
    }

    @Test
    void returnsAssistantContentWhenProviderDoesNotRequestTools() {
        LlmProviderRegistryRuntime registryRuntime = mock(LlmProviderRegistryRuntime.class);
        LlmProviderRegistry providerRegistry = mock(LlmProviderRegistry.class);
        ToolExecutionService toolExecutionService = mock(ToolExecutionService.class);
        AgentTraceService traceService = mock(AgentTraceService.class);
        LlmProvider provider = mock(LlmProvider.class);

        DefaultLlmService service = new DefaultLlmService(
                registryRuntime,
                toolExecutionService,
                new ObjectMapper(),
                traceService,
                new AgentTraceContextHolder()
        );

        when(registryRuntime.snapshot()).thenReturn(providerRegistry);
        when(providerRegistry.getRequired("provider")).thenReturn(provider);
        when(provider.chat(any(ChatRequest.class)))
                .thenReturn(new ChatResponse("model", new Message(MessageRole.ASSISTANT, "plain response")));

        String response = service.chat(
                "provider",
                "model",
                "system",
                "user",
                List.of()
        );

        assertThat(response).isEqualTo("plain response");
        verifyNoInteractions(toolExecutionService);
    }

    @Test
    void emitsToolCallEventWithSuccessFalseWhenToolExecutionFails() throws Exception {
        LlmProviderRegistryRuntime registryRuntime = mock(LlmProviderRegistryRuntime.class);
        LlmProviderRegistry providerRegistry = mock(LlmProviderRegistry.class);
        ToolExecutionService toolExecutionService = mock(ToolExecutionService.class);
        AgentTraceService traceService = mock(AgentTraceService.class);
        LlmProvider provider = mock(LlmProvider.class);
        AgentTraceContextHolder contextHolder = new AgentTraceContextHolder();
        contextHolder.setCurrentCorrelation(new AgentTraceCorrelation("run-1", "task-1", "agent-1", "message-1"));

        DefaultLlmService service = new DefaultLlmService(
                registryRuntime,
                toolExecutionService,
                new ObjectMapper(),
                traceService,
                contextHolder
        );

        ToolCall toolCall = new ToolCall("call-1", new ToolFunction("read_file", Map.of("path", "README.md")));
        ChatMessage toolMessage = new ChatMessage(MessageRole.ASSISTANT, "", List.of(toolCall));
        ChatMessage finalMessage = new ChatMessage(MessageRole.ASSISTANT, "done", null);

        when(registryRuntime.snapshot()).thenReturn(providerRegistry);
        when(providerRegistry.getRequired("provider")).thenReturn(provider);
        when(provider.chat(any(ChatRequest.class)))
                .thenReturn(new ChatResponse("model", toolMessage))
                .thenReturn(new ChatResponse("model", finalMessage));
        when(toolExecutionService.execute(toolCall))
                .thenThrow(new IllegalStateException("boom"));

        String response = service.chat(
                "provider",
                "model",
                "system",
                "user",
                List.of(ToolDefinition.function(new ToolSpec("read_file", "Read a file", Map.of())))
        );

        assertThat(response).isEqualTo("done");

        ArgumentCaptor<Map<String, Object>> attributesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(traceService).trace(
                eq(AgentTraceEventType.TOOL_CALL),
                eq("llm_service"),
                any(AgentTraceCorrelation.class),
                attributesCaptor.capture()
        );
        assertThat(attributesCaptor.getValue()).containsEntry("success", false);
    }
}
