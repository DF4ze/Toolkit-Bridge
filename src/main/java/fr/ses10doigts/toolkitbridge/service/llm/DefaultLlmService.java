package fr.ses10doigts.toolkitbridge.service.llm;

import fr.ses10doigts.toolkitbridge.model.dto.llm.ChatRequest;
import fr.ses10doigts.toolkitbridge.model.dto.llm.ChatResponse;
import fr.ses10doigts.toolkitbridge.model.dto.llm.message.ChatMessage;
import fr.ses10doigts.toolkitbridge.model.dto.llm.message.Message;
import fr.ses10doigts.toolkitbridge.model.dto.llm.message.MessageRole;
import fr.ses10doigts.toolkitbridge.model.dto.llm.message.ToolResultMessage;
import fr.ses10doigts.toolkitbridge.model.dto.llm.tooling.ToolCall;
import fr.ses10doigts.toolkitbridge.model.dto.llm.tooling.ToolDefinition;
import fr.ses10doigts.toolkitbridge.service.llm.provider.LlmProvider;
import fr.ses10doigts.toolkitbridge.service.llm.provider.LlmProviderRegistry;
import fr.ses10doigts.toolkitbridge.model.dto.tool.ToolExecutionResult;
import fr.ses10doigts.toolkitbridge.service.tool.ToolExecutionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultLlmService implements LlmService {

    private static final int MAX_TOOL_ROUNDS = 20;
    private static final int MAX_TOOL_CALLS_PER_ROUND = 8;

    private final LlmProviderRegistry llmProviderRegistry;
    private final ToolExecutionService toolExecutionService;
    private final ObjectMapper objectMapper;


    @Override
    public String chat(String providerName, String model, String systemPrompt, String userMessage, List<ToolDefinition> toolDefinitions) {
        long startNanos = System.nanoTime();
        LlmProvider provider = llmProviderRegistry.getRequired(providerName);
        List<ToolDefinition> effectiveToolDefinitions = toolDefinitions == null ? List.of() : List.copyOf(toolDefinitions);
        boolean toolsEnabled = !effectiveToolDefinitions.isEmpty();

        log.debug("LLM request build provider={} model={} tools={} systemPromptLength={} userMessageLength={}",
                providerName,
                model,
                effectiveToolDefinitions.size(),
                systemPrompt == null ? 0 : systemPrompt.length(),
                userMessage == null ? 0 : userMessage.length());

        List<Message> messages = new ArrayList<>();
        messages.add(new Message(MessageRole.SYSTEM, systemPrompt));
        messages.add(new Message(MessageRole.USER, userMessage));

        int round = 0;
        while (true) {
            ChatRequest request = new ChatRequest(
                    model,
                    List.copyOf(messages),
                    effectiveToolDefinitions
            );

            ChatResponse response = provider.chat(request);
            Message responseMessage = response == null ? null : response.message();

            if (responseMessage instanceof ChatMessage chatMessage
                    && chatMessage.getToolCalls() != null
                    && !chatMessage.getToolCalls().isEmpty()) {

                if (!toolsEnabled) {
                    String content = chatMessage.getContent();
                    log.warn("Tool calls returned while tools are disabled provider={} model={} toolCalls={}",
                            providerName,
                            model,
                            chatMessage.getToolCalls().size());
                    if (content == null || content.isBlank()) {
                        return "Tool calls returned but tools are disabled for this agent.";
                    }
                    return content;
                }

                int toolCallsCount = chatMessage.getToolCalls().size();
                log.info("LLM tool calls received provider={} model={} round={} toolCalls={}",
                        providerName,
                        model,
                        round,
                        toolCallsCount);

                if (toolCallsCount > MAX_TOOL_CALLS_PER_ROUND) {
                    log.warn("LLM tool calls over limit provider={} model={} count={} limit={}",
                            providerName,
                            model,
                            toolCallsCount,
                            MAX_TOOL_CALLS_PER_ROUND);
                    return "Too many tool calls requested in a single response.";
                }

                if (round >= MAX_TOOL_ROUNDS) {
                    log.warn("LLM tool call loop limit reached provider={} model={} rounds={}",
                            providerName,
                            model,
                            MAX_TOOL_ROUNDS);
                    return "Tool execution loop limit reached.";
                }

                messages.add(chatMessage);
                for (ToolCall toolCall : chatMessage.getToolCalls()) {
                    ToolResultMessage toolResultMessage = executeToolCall(toolCall);
                    messages.add(toolResultMessage);
                }

                round++;
                continue;
            }

            String content = responseMessage == null ? null : responseMessage.getContent();
            log.info("LLM request completed provider={} model={} responseLength={} durationMs={}",
                    providerName,
                    model,
                    content == null ? 0 : content.length(),
                    elapsedMs(startNanos));
            return content;
        }
    }

    private ToolResultMessage executeToolCall(ToolCall toolCall) {
        if (toolCall == null || toolCall.function() == null) {
            return new ToolResultMessage(
                    serializeToolResult(ToolExecutionResult.builder()
                            .error(true)
                            .message("Invalid tool call payload")
                            .build()),
                    "unknown",
                    toolCall == null ? null : toolCall.id()
            );
        }

        String toolName = toolCall.function().name();
        String toolCallId = toolCall.id();
        Map<String, Object> arguments = toolCall.function().arguments();

        log.info("Executing tool call name={} id={} argsKeys={} argsPreview={}",
                toolName,
                toolCallId,
                arguments == null ? List.of() : arguments.keySet(),
                formatArgsPreview(arguments));

        ToolExecutionResult result;
        try {
            result = toolExecutionService.execute(toolCall);
        } catch (Exception e) {
            log.warn("Tool execution failed name={} id={}", toolName, toolCallId, e);
            result = ToolExecutionResult.builder()
                    .error(true)
                    .message("Tool execution failed: " + safeMessage(e))
                    .build();
        }

        return new ToolResultMessage(
                serializeToolResult(result),
                toolName,
                toolCallId
        );
    }

    private String serializeToolResult(ToolExecutionResult result) {
        ToolExecutionResult safe = result == null
                ? ToolExecutionResult.builder().error(true).message("Tool returned null result").build()
                : result;

        try {
            return objectMapper.writeValueAsString(safe);
        } catch (JacksonException e) {
            String fallback = safe.getMessage();
            return (fallback == null || fallback.isBlank())
                    ? "{\"error\":true,\"message\":\"Tool result serialization failed\"}"
                    : fallback;
        }
    }

    private String safeMessage(Throwable e) {
        if (e == null || e.getMessage() == null || e.getMessage().isBlank()) {
            return e == null ? "unknown error" : e.getClass().getSimpleName();
        }
        return e.getMessage();
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private String formatArgsPreview(Map<String, Object> arguments) {
        if (arguments == null || arguments.isEmpty()) {
            return "{}";
        }

        StringBuilder sb = new StringBuilder("{");
        int count = 0;
        for (Map.Entry<String, Object> entry : arguments.entrySet()) {
            if (count > 0) {
                sb.append(", ");
            }
            String key = entry.getKey();
            sb.append(key).append('=').append(sanitizeArgValue(key, entry.getValue()));
            count++;
            if (count >= 8) {
                sb.append(", ...");
                break;
            }
        }
        sb.append('}');
        return sb.toString();
    }

    private String sanitizeArgValue(String key, Object value) {
        if (value == null) {
            return "null";
        }

        String lowered = key == null ? "" : key.toLowerCase();
        if (lowered.contains("token") || lowered.contains("key") || lowered.contains("secret")
                || lowered.contains("password") || lowered.contains("authorization")) {
            return "***";
        }

        String text = String.valueOf(value);
        if (text.length() > 300) {
            return text.substring(0, 300) + "...";
        }
        return text;
    }
}
