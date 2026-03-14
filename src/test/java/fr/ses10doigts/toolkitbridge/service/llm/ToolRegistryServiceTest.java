package fr.ses10doigts.toolkitbridge.service.llm;

import fr.ses10doigts.toolkitbridge.model.dto.llm.LlmToolDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ToolRegistryServiceTest {

    private final ToolRegistryService toolRegistryService = new ToolRegistryService();

    @Test
    void listToolsShouldExposeRequestedTools() {
        List<LlmToolDefinition> tools = toolRegistryService.listTools();

        assertEquals(7, tools.size());

        Set<String> names = tools.stream().map(LlmToolDefinition::name).collect(Collectors.toSet());
        assertEquals(Set.of(
                "list_files",
                "read_file",
                "write_file",
                "append_file",
                "move_file",
                "delete_file",
                "run_command"
        ), names);
    }

    @Test
    void readFileToolShouldRequirePath() {
        LlmToolDefinition readFile = toolRegistryService.getTool("read_file").orElseThrow();

        @SuppressWarnings("unchecked")
        Map<String, Object> params = (Map<String, Object>) readFile.parameters();

        assertEquals("object", params.get("type"));
        assertEquals(List.of("path"), params.get("required"));
    }

    @Test
    void unknownToolShouldReturnEmpty() {
        assertTrue(toolRegistryService.getTool("unknown").isEmpty());
        assertTrue(toolRegistryService.getTool("").isEmpty());
        assertTrue(toolRegistryService.getTool(null).isEmpty());
    }
}
