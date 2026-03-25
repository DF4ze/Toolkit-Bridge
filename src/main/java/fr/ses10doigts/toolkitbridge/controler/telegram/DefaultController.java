package fr.ses10doigts.toolkitbridge.controler.telegram;

import fr.ses10doigts.telegrambots.model.TelegramUpdateContext;
import fr.ses10doigts.telegrambots.service.poller.handler.annot.Chat;
import fr.ses10doigts.telegrambots.service.poller.handler.annot.TelegramController;
import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentResponse;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.AgentRuntimeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@TelegramController
@RequiredArgsConstructor
@Slf4j
public class DefaultController {

    private final AgentRuntimeService agentRuntimeService;

    @Chat
    public String handleChatMessage( TelegramUpdateContext ctx ) {
        String text = ctx.getMessage().getText();
        if (text == null || text.isBlank()) {
            log.debug("Ignoring blank Telegram message chatId={} userId={}", ctx.getChatId(), ctx.getUserId());
            return null;
        }

        log.info("Telegram message received chatId={} userId={} length={}",
                ctx.getChatId(),
                ctx.getUserId(),
                text.length());
        log.debug("Telegram message preview chatId={} userId={} text='{}'",
                ctx.getChatId(),
                ctx.getUserId(),
                snippet(text));

        AgentResponse response = agentRuntimeService.processTelegramMessage(
                ctx.getChatId(),
                ctx.getUserId(),
                text
        );

        log.info("Telegram response ready chatId={} userId={} error={} length={}",
                ctx.getChatId(),
                ctx.getUserId(),
                response.error(),
                response.message() == null ? 0 : response.message().length());
        log.debug("Telegram response preview chatId={} userId={} text='{}'",
                ctx.getChatId(),
                ctx.getUserId(),
                snippet(response.message()));
        return response.message();
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
