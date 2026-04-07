package fr.ses10doigts.toolkitbridge.service.agent.trace.sink;

import fr.ses10doigts.toolkitbridge.service.agent.trace.config.AgentTraceProperties;
import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Component
@Slf4j
public class JsonLinesAgentTraceSink implements AgentTraceSink {

    private final AgentTraceProperties properties;
    private final ObjectMapper objectMapper;

    public JsonLinesAgentTraceSink(AgentTraceProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publish(AgentTraceEvent event) {
        if (!properties.getFile().isEnabled() || event == null) {
            return;
        }

        Path file = resolveFile(event);
        try {
            Files.createDirectories(file.getParent());
            String line = objectMapper.writeValueAsString(event) + System.lineSeparator();
            Files.writeString(file, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            log.warn("Failed to write agent trace file path={}", file, e);
        }
    }

    private Path resolveFile(AgentTraceEvent event) {
        String agentId = event.correlation() == null ? null : event.correlation().agentId();
        String safeAgentId = (agentId == null || agentId.isBlank())
                ? "unknown-agent"
                : agentId.replaceAll("[^A-Za-z0-9._-]", "_");
        return Path.of(properties.getFile().getRootPath()).resolve(safeAgentId + ".jsonl");
    }
}
