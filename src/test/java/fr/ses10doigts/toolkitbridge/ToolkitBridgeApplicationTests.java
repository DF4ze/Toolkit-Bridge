package fr.ses10doigts.toolkitbridge;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:sqlite:file:./target/test-db-${random.uuid}.db"
})
class ToolkitBridgeApplicationTests {

    @Test
    void contextLoads() {
    }

}
