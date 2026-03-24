package fr.ses10doigts.toolkitbridge.controler.telegram;

import fr.ses10doigts.telegrambots.model.TelegramUpdateContext;
import fr.ses10doigts.telegrambots.service.poller.handler.annot.Chat;
import fr.ses10doigts.telegrambots.service.poller.handler.annot.TelegramController;
import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentResponse;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.AgentRuntimeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@TelegramController
@RequiredArgsConstructor
public class DefaultController {

    private AgentRuntimeService agentRuntimeService;

    @Chat
    public String handleChatMessage( TelegramUpdateContext ctx ) {
        String text = ctx.getMessage().getText();
        if (text == null || text.isBlank()) {
            return null;
        }

        AgentResponse response = agentRuntimeService.processTelegramMessage(
                ctx.getChatId(),
                ctx.getUserId(),
                ctx.getMessage().getText()
        );
        return response.message();
    }
}
