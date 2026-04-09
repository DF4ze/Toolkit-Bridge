package fr.ses10doigts.toolkitbridge.service.configuration.admin;

import fr.ses10doigts.toolkitbridge.ToolkitBridgeApplication;
import fr.ses10doigts.toolkitbridge.memory.config.runtime.MemoryRuntimeConfigurationResolver;
import fr.ses10doigts.toolkitbridge.service.configuration.admin.payload.MemoryConfigurationPayload;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        classes = ToolkitBridgeApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        properties = {
                "spring.datasource.url=jdbc:sqlite:file:./target/startup-order-it-${random.uuid}.db",
                "spring.datasource.driver-class-name=org.sqlite.JDBC",
                "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
                "spring.jpa.hibernate.ddl-auto=update",
                "spring.sql.init.mode=never",
                "telegram.enabled=false",
                "toolkit.memory.retrieval.max-candidate-pool-size=77"
        }
)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class AdministrableConfigurationStartupOrderIT {

    @Autowired
    private AdministrableConfigurationGateway configurationGateway;

    @Autowired
    private MemoryRuntimeConfigurationResolver runtimeConfigurationResolver;

    @Test
    void shouldBootstrapMemoryConfigurationThenInitializeRuntimeSnapshotFromDatabase() {
        Optional<MemoryConfigurationPayload> fromDb = configurationGateway.loadMemoryConfiguration();

        assertThat(fromDb).isPresent();
        assertThat(fromDb.get().getRetrieval().getMaxCandidatePoolSize()).isEqualTo(77);
        assertThat(runtimeConfigurationResolver.snapshot().retrieval().maxCandidatePoolSize()).isEqualTo(77);
    }
}
