package fr.ses10doigts.toolkitbridge.service.agent.runtime;

import fr.ses10doigts.telegrambots.service.bot.CurrentTelegramBotContext;
import fr.ses10doigts.telegrambots.service.bot.TelegramBotRegistry;
import fr.ses10doigts.toolkitbridge.exception.AgentRuntimeException;
import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentRequest;
import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentResponse;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.service.agent.definition.AgentDefinitionService;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.AgentOrchestrator;
import fr.ses10doigts.toolkitbridge.service.agent.orchestrator.AgentOrchestratorRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class AgentRuntimeService {

    private static final int MAX_MESSAGE_LENGTH = 8_000;
    private static final String CHANNEL_TELEGRAM = "telegram";

    private final CurrentTelegramBotContext currentBotService;
    private final TelegramBotRegistry telegramBotRegistry;
    private final AgentDefinitionService agentDefinitionService;
    private final AgentOrchestratorRegistry agentOrchestratorRegistry;

    public AgentResponse processTelegramMessage(Long chatId, Long userId, String message) {
        validateTelegramInput(chatId, userId, message);

        String telegramBotId = resolveCurrentTelegramBotId();

        AgentDefinition agentDefinition = agentDefinitionService.findByTelegramBotId(telegramBotId)
                .orElseThrow(() -> new AgentRuntimeException("No agent configured for current telegram bot"));

        AgentRequest request = buildTelegramRequest(agentDefinition, chatId, userId, message);

        AgentOrchestrator orchestrator = agentOrchestratorRegistry.getByType(agentDefinition.orchestratorType());

        try {
            AgentResponse response = orchestrator.handle(agentDefinition, request);
            return normalizeResponse(response);
        } catch (AgentRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new AgentRuntimeException("Agent execution failed", e);
        }
    }

    private void validateTelegramInput(Long chatId, Long userId, String message) {
        if (chatId == null) {
            throw new AgentRuntimeException("chatId must not be null");
        }
        if (userId == null) {
            throw new AgentRuntimeException("userId must not be null");
        }
        if (message == null || message.isBlank()) {
            throw new AgentRuntimeException("message must not be blank");
        }
        if (message.length() > MAX_MESSAGE_LENGTH) {
            throw new AgentRuntimeException("message is too long");
        }
    }

    private String resolveCurrentTelegramBotId() {
        String currentBotId = currentBotService.getCurrentBotId();
        if (currentBotId == null || currentBotId.isBlank()) {
            throw new AgentRuntimeException("No current telegram bot available");
        }
        return currentBotId;
    }

    private AgentRequest buildTelegramRequest(AgentDefinition agentDefinition,
                                              Long chatId,
                                              Long userId,
                                              String message) {
        Map<String, Object> context = Map.of(
                "telegramBotId", agentDefinition.telegramBotId(),
                "chatId", String.valueOf(chatId),
                "userId", String.valueOf(userId)
        );

        return new AgentRequest(
                agentDefinition.id(),
                CHANNEL_TELEGRAM,
                String.valueOf(userId),
                String.valueOf(chatId),
                message.trim(),
                context
        );
    }

    private AgentResponse normalizeResponse(AgentResponse response) {
        if (response == null) {
            return AgentResponse.error("Empty agent response");
        }
        if (response.message() == null || response.message().isBlank()) {
            return AgentResponse.error("Empty agent response");
        }
        return response;
    }
}