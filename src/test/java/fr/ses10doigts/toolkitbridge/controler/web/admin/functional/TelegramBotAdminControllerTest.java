package fr.ses10doigts.toolkitbridge.controler.web.admin.functional;

import fr.ses10doigts.toolkitbridge.model.dto.admin.telegram.TelegramBotAdminCreateRequest;
import fr.ses10doigts.toolkitbridge.model.dto.admin.telegram.TelegramBotAdminCreateResponse;
import fr.ses10doigts.toolkitbridge.model.dto.admin.telegram.TelegramBotAdminResponse;
import fr.ses10doigts.toolkitbridge.service.admin.functional.TelegramBotAdminFacade;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TelegramBotAdminControllerTest {

    @Test
    void exposesReadEndpointsAndReturnsNotImplementedForCreate() {
        TelegramBotAdminFacade facade = mock(TelegramBotAdminFacade.class);
        TelegramBotAdminController controller = new TelegramBotAdminController(facade);

        TelegramBotAdminResponse bot = new TelegramBotAdminResponse("bot-1", true, 123L, true, true);
        TelegramBotAdminCreateResponse createPayload = new TelegramBotAdminCreateResponse(
                "Telegram bot provisioning is not implemented yet. Requested botId=bot-2"
        );
        when(facade.listTelegramBots()).thenReturn(List.of(bot));
        when(facade.getTelegramBot("bot-1")).thenReturn(Optional.of(bot));
        when(facade.createTelegramBot("bot-2")).thenReturn(createPayload);

        assertThat(controller.listTelegramBots()).containsExactly(bot);

        ResponseEntity<TelegramBotAdminResponse> getResponse = controller.getTelegramBot("bot-1");
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isEqualTo(bot);

        ResponseEntity<TelegramBotAdminCreateResponse> createResponse =
                controller.createTelegramBot(new TelegramBotAdminCreateRequest("bot-2"));
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_IMPLEMENTED);
        assertThat(createResponse.getBody()).isEqualTo(createPayload);

        verify(facade).listTelegramBots();
        verify(facade).getTelegramBot("bot-1");
        verify(facade).createTelegramBot("bot-2");
        verifyNoMoreInteractions(facade);
    }
}
