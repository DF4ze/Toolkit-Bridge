package fr.ses10doigts.toolkitbridge.controler.web;

import fr.ses10doigts.toolkitbridge.model.dto.llm.tooling.ToolCall;
import fr.ses10doigts.toolkitbridge.model.dto.llm.tooling.ToolFunction;
import fr.ses10doigts.toolkitbridge.model.dto.tool.ToolExecutionResult;
import fr.ses10doigts.toolkitbridge.service.tool.ToolExecutionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class AgentFileController {

    private final ToolExecutionService toolExecutionService;

    @GetMapping("/list")
    public ToolExecutionResult listFiles(@RequestParam(required = false) String path)
            throws Exception {
        Map<String, Object> arguments = new HashMap<>();
        if (path != null && !path.isBlank()) {
            arguments.put("path", path);
        }
        return execute("list_files", arguments);
    }

    @GetMapping("/read")
    public ToolExecutionResult readFile(@RequestParam String path)
            throws Exception {
        return execute("read_file", Map.of("path", path));
    }

    @PostMapping("/write")
    public ToolExecutionResult writeFile(@RequestParam String path, @RequestBody String content)
            throws Exception {
        return execute("write_file", Map.of(
                "path", path,
                "content", content
        ));
    }

    @PostMapping("/append")
    public ToolExecutionResult appendFile(@RequestParam String path, @RequestBody String content)
            throws Exception {
        return execute("append_file", Map.of(
                "path", path,
                "content", content
        ));
    }

    @DeleteMapping("/delete")
    public ToolExecutionResult deleteFile(@RequestParam String path)
            throws Exception {
        return execute("delete_file", Map.of("path", path));
    }

    @PostMapping("/move")
    public ToolExecutionResult moveFile(@RequestParam String sourcePath, @RequestParam String targetPath)
            throws Exception {
        return execute("move_file", Map.of(
                "source_path", sourcePath,
                "target_path", targetPath
        ));
    }

    private ToolExecutionResult execute(String toolName, Map<String, Object> arguments) throws Exception {
        return toolExecutionService.execute(
                new ToolCall("web-" + toolName, new ToolFunction(toolName, arguments))
        );
    }
}
