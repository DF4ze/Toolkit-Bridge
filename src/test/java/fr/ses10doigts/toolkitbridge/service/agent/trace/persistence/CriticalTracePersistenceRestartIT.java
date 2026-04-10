package fr.ses10doigts.toolkitbridge.service.agent.trace.persistence;

import fr.ses10doigts.toolkitbridge.ToolkitBridgeApplication;
import fr.ses10doigts.toolkitbridge.model.dto.admin.technical.TechnicalAdminView;
import fr.ses10doigts.toolkitbridge.service.admin.TraceQueryService;
import fr.ses10doigts.toolkitbridge.service.agent.trace.AgentTraceService;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceCorrelation;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEventType;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CriticalTracePersistenceRestartIT {

    @Test
    void shouldKeepCriticalTracesAfterContextRestart() throws Exception {
        Path databasePath = Files.createTempFile("critical-trace-restart-", ".db");
        Files.deleteIfExists(databasePath);
        String datasourceUrl = "jdbc:sqlite:file:" + databasePath.toAbsolutePath().toString().replace("\\", "/");

        try (ConfigurableApplicationContext context = appContext(datasourceUrl)) {
            AgentTraceService traceService = context.getBean(AgentTraceService.class);
            traceService.trace(
                    AgentTraceEventType.ERROR,
                    "message_bus",
                    new AgentTraceCorrelation("run-r1", "task-r1", "agent-r1", "msg-r1"),
                    Map.of("category", "delegation", "reason", "dispatch_failed")
            );
        }

        try (ConfigurableApplicationContext context = appContext(datasourceUrl)) {
            TraceQueryService traceQueryService = context.getBean(TraceQueryService.class);
            List<TechnicalAdminView.TraceItem> traces = traceQueryService.listRecentTraces(10, "agent-r1");

            assertThat(traces).hasSize(1);
            assertThat(traces.get(0).type()).isEqualTo(AgentTraceEventType.ERROR);
            assertThat(traces.get(0).runId()).isEqualTo("run-r1");
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
                        "--toolkit.observability.agent-tracing.memory.enabled=false",
                        "--toolkit.observability.agent-tracing.file.enabled=false",
                        "--toolkit.llm.openai-like.providers[0].name=seed",
                        "--toolkit.llm.openai-like.providers[0].base-url=http://localhost:11434/v1",
                        "--toolkit.llm.openai-like.providers[0].api-key=",
                        "--toolkit.llm.openai-like.providers[0].default-model=qwen3.5:9b"
                );
    }
}
