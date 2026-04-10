package fr.ses10doigts.toolkitbridge.service.admin.task;

import fr.ses10doigts.toolkitbridge.ToolkitBridgeApplication;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.Task;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskEntryPoint;
import fr.ses10doigts.toolkitbridge.service.agent.task.model.TaskStatus;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class PersistentAdminTaskStoreRestartIT {

    @Test
    void shouldKeepSnapshotsAfterContextRestart() throws Exception {
        Path databasePath = Files.createTempFile("admin-task-restart-", ".db");
        Files.deleteIfExists(databasePath);
        String datasourceUrl = "jdbc:sqlite:file:" + databasePath.toAbsolutePath().toString().replace("\\", "/");

        String taskIdA = "task-restart-a";
        String taskIdB = "task-restart-b";

        try (ConfigurableApplicationContext context = appContext(datasourceUrl)) {
            AdminTaskStore store = context.getBean(AdminTaskStore.class);
            store.record(task(taskIdA, "agent-a", TaskStatus.CREATED), "telegram", "conv-a", null);
            store.record(task(taskIdB, "agent-b", TaskStatus.RUNNING), "telegram", "conv-b", null);
        }

        try (ConfigurableApplicationContext context = appContext(datasourceUrl)) {
            AdminTaskStore store = context.getBean(AdminTaskStore.class);
            List<AdminTaskSnapshot> snapshots = store.recent(10, null, null);

            assertThat(snapshots).extracting(AdminTaskSnapshot::taskId).contains(taskIdA, taskIdB);
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

    private Task task(String taskId, String assignedAgentId, TaskStatus status) {
        return new Task(
                taskId,
                "objective-" + taskId,
                "user-1",
                assignedAgentId,
                null,
                "trace-" + taskId,
                TaskEntryPoint.TASK_ORCHESTRATOR,
                status,
                Map.of(),
                List.of()
        );
    }
}
