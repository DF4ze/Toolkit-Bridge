package fr.ses10doigts.toolkitbridge.service.agent.runtime;

import fr.ses10doigts.telegrambots.service.bot.CurrentTelegramBotContext;
import fr.ses10doigts.toolkitbridge.exception.AgentRuntimeException;
import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentRequest;
import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentResponse;
import fr.ses10doigts.toolkitbridge.model.dto.agent.definition.AgentDefinition;
import fr.ses10doigts.toolkitbridge.model.dto.auth.AuthenticatedAgent;
import fr.ses10doigts.toolkitbridge.service.agent.definition.AgentDefinitionService;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.model.AgentRuntime;
import fr.ses10doigts.toolkitbridge.service.auth.AgentAccountService;
import fr.ses10doigts.toolkitbridge.service.auth.AgentContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentRuntimeService {

    private static final int MAX_MESSAGE_LENGTH = 8_000;
    private static final String CHANNEL_TELEGRAM = "telegram";

    private final ObjectProvider<CurrentTelegramBotContext> currentBotServiceProvider;
    private final AgentDefinitionService agentDefinitionService;
    private final AgentRuntimeRegistry runtimeRegistry;
    private final AgentAccountService agentAccountService;
    private final AgentContextHolder agentContextHolder;

    public AgentResponse processTelegramMessage(Long chatId, Long userId, String message) {
        String traceId = newTraceId();
        long startNanos = System.nanoTime();
        validateTelegramInput(chatId, userId, message);

        log.info("Runtime start traceId={} channel={} chatId={} userId={} length={}",
                traceId,
                CHANNEL_TELEGRAM,
                chatId,
                userId,
                message.length());
        log.debug("Runtime message preview traceId={} text='{}'", traceId, snippet(message));

        String telegramBotId = resolveCurrentTelegramBotId();
        log.debug("Runtime resolved telegramBotId traceId={} botId={}", traceId, telegramBotId);

        AgentDefinition agentDefinition = agentDefinitionService.findByTelegramBotId(telegramBotId)
                .orElseThrow(() -> new AgentRuntimeException("No agent configured for current telegram bot"));

        log.info("Runtime resolved agent traceId={} agentId={} name={} orchestrator={}",
                traceId,
                agentDefinition.id(),
                agentDefinition.name(),
                agentDefinition.orchestratorType());

        AgentRequest request = buildTelegramRequest(agentDefinition, chatId, userId, message, traceId);

        AuthenticatedAgent authenticatedAgent = authenticateTelegramAgent(agentDefinition, traceId);
        AgentRuntime runtime = runtimeRegistry.getOrCreate(agentDefinition, authenticatedAgent);
        runtime.state().startExecution(
                traceId,
                request.channelType(),
                request.channelConversationId(),
                "orchestrator:" + runtime.orchestrator().getType().name().toLowerCase(),
                request.channelConversationId()
        );
        agentContextHolder.setCurrentBot(authenticatedAgent);

        try {
            log.info("Runtime delegating to orchestrator traceId={} type={}", traceId, runtime.orchestrator().getType());

            AgentResponse response = runtime.orchestrator().handle(runtime, request);
            AgentResponse normalized = normalizeResponse(response);

            log.info("Runtime completed traceId={} error={} responseLength={} durationMs={}",
                    traceId,
                    normalized.error(),
                    normalized.message() == null ? 0 : normalized.message().length(),
                    elapsedMs(startNanos));
            log.debug("Runtime response preview traceId={} text='{}'", traceId, snippet(normalized.message()));

            return normalized;

        } catch (AgentRuntimeException e) {
            log.warn("Runtime failed traceId={} durationMs={}", traceId, elapsedMs(startNanos), e);
            throw e;
        } catch (Exception e) {
            log.error("Runtime unexpected failure traceId={} durationMs={}", traceId, elapsedMs(startNanos), e);
            throw new AgentRuntimeException("Agent execution failed", e);
        } finally {
            runtime.state().finishExecution();
            agentContextHolder.clear();
            log.debug("Runtime finished traceId={} durationMs={}", traceId, elapsedMs(startNanos));
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
        CurrentTelegramBotContext currentBotService = currentBotServiceProvider.getIfAvailable();
        if (currentBotService == null) {
            throw new AgentRuntimeException("Telegram runtime is unavailable because telegram support is disabled");
        }
        String currentBotId = currentBotService.getCurrentBotId();
        if (currentBotId == null || currentBotId.isBlank()) {
            throw new AgentRuntimeException("No current telegram bot available");
        }
        return currentBotId;
    }

    private AgentRequest buildTelegramRequest(AgentDefinition agentDefinition,
                                              Long chatId,
                                              Long userId,
                                              String message,
                                              String traceId) {
        Map<String, Object> context = Map.of(
                "telegramBotId", agentDefinition.telegramBotId(),
                "chatId", String.valueOf(chatId),
                "userId", String.valueOf(userId),
                "traceId", traceId
        );

        return new AgentRequest(
                agentDefinition.id(),
                CHANNEL_TELEGRAM,
                String.valueOf(userId),
                String.valueOf(chatId),
                null,
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

    private AuthenticatedAgent authenticateTelegramAgent(AgentDefinition agentDefinition, String traceId) {
        try {
            AuthenticatedAgent authenticated = agentAccountService.authenticateByAgentIdent(agentDefinition.id());
            log.debug("Runtime authenticated agent traceId={} agentId={}", traceId, authenticated.agentIdent());
            return authenticated;
        } catch (Exception e) {
            log.warn("Runtime failed to authenticate agent for telegram traceId={} agentId={}",
                    traceId,
                    agentDefinition == null ? null : agentDefinition.id(),
                    e);
            throw new AgentRuntimeException("Agent authentication failed for telegram", e);
        }
    }

    private String newTraceId() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private String snippet(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 160) {
            return trimmed;
        }
        return trimmed.substring(0, 160) + "...";
    }
}
