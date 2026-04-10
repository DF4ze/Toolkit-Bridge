package fr.ses10doigts.toolkitbridge.service.agent.supervision.human;

import fr.ses10doigts.toolkitbridge.ToolkitBridgeApplication;
import fr.ses10doigts.toolkitbridge.service.agent.policy.AgentSensitiveAction;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class PersistentHumanInterventionServiceRestartIT {

    @Test
    void shouldKeepInterventionAfterContextRestart() throws Exception {
        Path databasePath = Files.createTempFile("human-intervention-restart-", ".db");
        Files.deleteIfExists(databasePath);
        String datasourceUrl = "jdbc:sqlite:file:" + databasePath.toAbsolutePath().toString().replace("\\", "/");

        String requestId;
        try (ConfigurableApplicationContext context = appContext(datasourceUrl)) {
            HumanInterventionService service = context.getBean(HumanInterventionService.class);
            HumanInterventionRequest opened = service.open(new HumanInterventionDraft(
                    "trace-restart-1",
                    "agent-restart",
                    AgentSensitiveAction.TOOL_EXECUTION,
                    HumanInterventionKind.APPROVAL,
                    "Review restart persistence",
                    null,
                    java.util.Map.of("scope", "restart")
            ));
            service.recordDecision(new HumanInterventionDecision(
                    opened.requestId(),
                    HumanInterventionStatus.APPROVED,
                    Instant.now(),
                    "operator-restart",
                    "manual",
                    "approved",
                    java.util.Map.of("reason", "restart-test")
            ));
            requestId = opened.requestId();
        }

        try (ConfigurableApplicationContext context = appContext(datasourceUrl)) {
            HumanInterventionService service = context.getBean(HumanInterventionService.class);
            Optional<HumanInterventionRequest> reloaded = service.findById(requestId);
            assertThat(reloaded).isPresent();
            assertThat(reloaded.orElseThrow().status()).isEqualTo(HumanInterventionStatus.APPROVED);
        }
    }

    private ConfigurableApplicationContext appContext(String datasourceUrl) {
        return new SpringApplicationBuilder(ToolkitBridgeApplication.class)
                .web(WebApplicationType.NONE)
                .run(
                        "--spring.datasource.url=" + datasourceUrl,
                        "--spring.datasource.driver-class-name=org.sqlite.JDBC",
                        "--spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
                        "--spring.jpa.hibernate.ddl-auto=update",
                        "--spring.sql.init.mode=never",
                        "--telegram.enabled=false",
                        "--toolkit.llm.openai-like.providers[0].name=seed",
                        "--toolkit.llm.openai-like.providers[0].base-url=http://localhost:11434/v1",
                        "--toolkit.llm.openai-like.providers[0].api-key=",
                        "--toolkit.llm.openai-like.providers[0].default-model=qwen3.5:9b"
                );
    }
}
