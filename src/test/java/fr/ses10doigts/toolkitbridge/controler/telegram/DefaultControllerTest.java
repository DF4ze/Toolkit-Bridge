package fr.ses10doigts.toolkitbridge.controler.telegram;

import fr.ses10doigts.telegrambots.model.TelegramUpdateContext;
import fr.ses10doigts.toolkitbridge.model.dto.agent.comm.AgentResponse;
import fr.ses10doigts.toolkitbridge.service.agent.runtime.AgentRuntimeService;
import fr.ses10doigts.toolkitbridge.service.agent.supervision.telegram.TelegramSupervisionChatGuard;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DefaultControllerTest {

    @Test
    void ignoresMessagesFromReadOnlySupervisionChat() {
        AgentRuntimeService runtimeService = mock(AgentRuntimeService.class);
        TelegramSupervisionChatGuard guard = mock(TelegramSupervisionChatGuard.class);
        DefaultController controller = new DefaultController(runtimeService, guard);

        TelegramUpdateContext context = new TelegramUpdateContext(
                "supervision-bot",
                null,
                null,
                42L,
                10L,
                "hello",
                null,
                null,
                java.util.List.of(),
                false,
                null
        );
        when(guard.isReadOnlySupervisionChat("supervision-bot", 42L)).thenReturn(true);

        String response = controller.handleChatMessage(context);

        assertThat(response).isNull();
        verify(runtimeService, never()).processTelegramMessage(42L, 10L, "hello");
    }

    @Test
    void delegatesRegularTelegramMessagesToRuntime() {
        AgentRuntimeService runtimeService = mock(AgentRuntimeService.class);
        TelegramSupervisionChatGuard guard = mock(TelegramSupervisionChatGuard.class);
        DefaultController controller = new DefaultController(runtimeService, guard);

        TelegramUpdateContext context = new TelegramUpdateContext(
                "agent-bot",
                null,
                null,
                100L,
                200L,
                "ping",
                null,
                null,
                java.util.List.of(),
                false,
                null
        );
        when(guard.isReadOnlySupervisionChat("agent-bot", 100L)).thenReturn(false);
        when(runtimeService.processTelegramMessage(100L, 200L, "ping"))
                .thenReturn(AgentResponse.success("pong"));

        String response = controller.handleChatMessage(context);

        assertThat(response).isEqualTo("pong");
        verify(runtimeService).processTelegramMessage(100L, 200L, "ping");
    }
}
