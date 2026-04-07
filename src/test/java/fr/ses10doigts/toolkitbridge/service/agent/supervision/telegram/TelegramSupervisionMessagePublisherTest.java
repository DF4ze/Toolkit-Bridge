package fr.ses10doigts.toolkitbridge.service.agent.supervision.telegram;

import fr.ses10doigts.telegrambots.service.sender.TelegramSenderRegistry;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;

class TelegramSupervisionMessagePublisherTest {

    @Test
    void isNotConfiguredWhenTelegramSupportIsDisabled() {
        TelegramSupervisionProperties properties = new TelegramSupervisionProperties();
        properties.setEnabled(true);
        properties.setChatId(12L);

        TelegramSupervisionMessagePublisher publisher = new TelegramSupervisionMessagePublisher(
                properties,
                Optional.of(mock(TelegramSenderRegistry.class)),
                false
        );

        assertThat(publisher.isConfigured()).isFalse();
        assertThatCode(() -> publisher.publish("hello")).doesNotThrowAnyException();
    }

    @Test
    void isNotConfiguredWhenSenderRegistryIsUnavailable() {
        TelegramSupervisionProperties properties = new TelegramSupervisionProperties();
        properties.setEnabled(true);
        properties.setChatId(12L);

        TelegramSupervisionMessagePublisher publisher = new TelegramSupervisionMessagePublisher(
                properties,
                Optional.empty(),
                true
        );

        assertThat(publisher.isConfigured()).isFalse();
        assertThatCode(() -> publisher.publish("hello")).doesNotThrowAnyException();
    }
}
