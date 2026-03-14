package fr.ses10doigts.toolkitbridge.service.llm;

import fr.ses10doigts.toolkitbridge.model.dto.llm.OllamaToolDefinition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class ToolRegistryServiceTest {

    private final ToolRegistryService toolRegistryService = new ToolRegistryService();

    @Test
    void listToolsShouldExposeAllSupportedTools() {
        List<OllamaToolDefinition> tools = toolRegistryService.listTools();

        assertEquals(7, tools.size());

        Set<String> names = tools.stream()
                .map(tool -> tool.function().name())
                .collect(Collectors.toSet());

        assertTrue(names.contains("list_files"));
        assertTrue(names.contains("read_file"));
        assertTrue(names.contains("write_file"));
        assertTrue(names.contains("append_file"));
        assertTrue(names.contains("move_file"));
        assertTrue(names.contains("delete_file"));
        assertTrue(names.contains("run_command"));
    }

    @Test
    void findByNameShouldReturnToolDefinitionWhenExists() {
        OllamaToolDefinition readFile = toolRegistryService.findByName("read_file")
                .orElseThrow();

        assertEquals("function", readFile.type());
        assertEquals("read_file", readFile.function().name());
        assertNotNull(readFile.function().parameters());
    }

    @Test
    void findByNameShouldReturnEmptyWhenUnknown() {
        assertTrue(toolRegistryService.findByName("unknown_tool").isEmpty());
    }
}
