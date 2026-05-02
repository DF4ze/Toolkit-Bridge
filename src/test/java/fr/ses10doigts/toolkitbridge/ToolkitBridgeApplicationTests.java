package fr.ses10doigts.toolkitbridge;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:file:./target/test-db-${random.uuid}.db",
        "telegram.enabled=false",
        "spring.autoconfigure.exclude=fr.ses10doigts.telegrambots.configuration.TelegramAutoConfiguration"
})
class ToolkitBridgeApplicationTests {

    @Test
    void contextLoads() {
    }

}
