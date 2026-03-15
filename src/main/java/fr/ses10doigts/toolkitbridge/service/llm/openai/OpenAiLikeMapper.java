package fr.ses10doigts.toolkitbridge.service.llm.openai;

import fr.ses10doigts.toolkitbridge.model.dto.llm.ChatRequest;
import fr.ses10doigts.toolkitbridge.model.dto.llm.ChatResponse;
import fr.ses10doigts.toolkitbridge.model.dto.llm.message.ChatMessage;
import fr.ses10doigts.toolkitbridge.model.dto.llm.message.Message;
import fr.ses10doigts.toolkitbridge.model.dto.llm.message.MessageRole;
import fr.ses10doigts.toolkitbridge.model.dto.llm.message.ToolResultMessage;
import fr.ses10doigts.toolkitbridge.model.dto.llm.provider.openai.*;
import fr.ses10doigts.toolkitbridge.model.dto.llm.tool.ToolCall;
import fr.ses10doigts.toolkitbridge.model.dto.llm.tool.ToolDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.llm.tool.ToolFunction;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Component
public class OpenAiLikeMapper {

    private final ObjectMapper objectMapper;

    public OpenAiLikeMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public OpenAiChatRequest toOpenAiChatRequest(String model, ChatRequest request) {
        return new OpenAiChatRequest(
                model,
                request.messages().stream()
                        .map(this::toOpenAiMessage)
                        .toList(),
                request.tools().isEmpty() ? null : request.tools().stream()
                        .map(this::toOpenAiToolDefinition)
                        .toList()
        );
    }

    public ChatResponse toChatResponse(OpenAiChatResponse response) {
        if (response.choices() == null || response.choices().isEmpty()) {
            throw new IllegalStateException("No choices returned by provider");
        }

        OpenAiMessage openAiMessage = response.choices().getFirst().message();
        Message message = toInternalMessage(openAiMessage);

        return new ChatResponse(response.model(), message);
    }

    private OpenAiMessage toOpenAiMessage(Message message) {
        if (message instanceof ChatMessage chatMessage) {
            return new OpenAiMessage(
                    MessageRole.toRole(chatMessage.getRole()),
                    chatMessage.getContent(),
                    null,
                    null,
                    chatMessage.getToolCalls() == null ? List.of() : chatMessage.getToolCalls().stream()
                            .map(this::toOpenAiToolCall)
                            .toList()
            );
        }

        if (message instanceof ToolResultMessage toolResultMessage) {
            return new OpenAiMessage(
                    MessageRole.toRole(MessageRole.TOOL),
                    toolResultMessage.getContent(),
                    toolResultMessage.getToolName(),
                    toolResultMessage.getToolCallId(),
                    null
            );
        }

        return new OpenAiMessage(
                MessageRole.toRole(message.getRole()),
                message.getContent(),
                null,
                null,
                null
        );
    }

    private Message toInternalMessage(OpenAiMessage message) {
        MessageRole role = MessageRole.fromRole(message.role());

        if (role == MessageRole.TOOL) {
            return new ToolResultMessage(
                    message.content(),
                    message.name(),
                    message.tool_call_id()
            );
        }

        List<ToolCall> toolCalls = message.tool_calls() == null ? List.of() : message.tool_calls().stream()
                .map(this::toInternalToolCall)
                .toList();

        return new ChatMessage(
                role,
                message.content(),
                toolCalls
        );
    }

    private OpenAiToolDefinition toOpenAiToolDefinition(ToolDefinition toolDefinition) {
        return new OpenAiToolDefinition(
                "function",
                new OpenAiToolSpec(
                        toolDefinition.function().name(),
                        toolDefinition.function().description(),
                        toolDefinition.function().parameters()
                )
        );
    }

    private OpenAiToolCall toOpenAiToolCall(ToolCall toolCall) {
        return new OpenAiToolCall(
                toolCall.id(),
                "function",
                new OpenAiToolFunction(
                        toolCall.function().name(),
                        writeArguments(toolCall.function().arguments())
                )
        );
    }

    private ToolCall toInternalToolCall(OpenAiToolCall toolCall) {
        return new ToolCall(
                toolCall.id(),
                new ToolFunction(
                        toolCall.function().name(),
                        readArguments(toolCall.function().arguments())
                )
        );
    }

    private String writeArguments(Map<String, Object> arguments) {
        try {
            return objectMapper.writeValueAsString(arguments == null ? Map.of() : arguments);
        } catch (JacksonException e) {
            throw new IllegalStateException("Unable to serialize tool arguments", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readArguments(String argumentsJson) {
        try {
            if (argumentsJson == null || argumentsJson.isBlank()) {
                return Map.of();
            }
            return objectMapper.readValue(argumentsJson, Map.class);
        } catch (JacksonException e) {
            throw new IllegalStateException("Unable to deserialize tool arguments", e);
        }
    }


}