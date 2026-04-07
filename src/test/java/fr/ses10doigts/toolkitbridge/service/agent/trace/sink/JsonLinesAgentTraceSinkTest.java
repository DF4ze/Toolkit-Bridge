package fr.ses10doigts.toolkitbridge.service.agent.trace.sink;

import fr.ses10doigts.toolkitbridge.service.agent.trace.config.AgentTraceProperties;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceCorrelation;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEvent;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEventType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class JsonLinesAgentTraceSinkTest {

    @TempDir
    Path tempDir;

    @Test
    void writesOneJsonLinePerAgent() throws Exception {
        AgentTraceProperties properties = new AgentTraceProperties();
        properties.getFile().setRootPath(tempDir.toString());
        JsonLinesAgentTraceSink sink = new JsonLinesAgentTraceSink(properties, new ObjectMapper());

        sink.publish(new AgentTraceEvent(
                Instant.parse("2026-04-07T12:00:00Z"),
                AgentTraceEventType.TOOL_CALL,
                "llm_service",
                new AgentTraceCorrelation("run-1", "task-1", "agent-1", "message-1"),
                Map.of("toolName", "read_file", "success", true)
        ));

        Path output = tempDir.resolve("agent-1.jsonl");
        assertThat(Files.exists(output)).isTrue();
        String content = Files.readString(output);
        assertThat(content).contains("\"type\":\"TOOL_CALL\"");
        assertThat(content).contains("\"toolName\":\"read_file\"");
    }
}
