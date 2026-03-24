package fr.ses10doigts.toolkitbridge.controler.web;

import fr.ses10doigts.toolkitbridge.model.dto.tool.ToolExecutionResult;
import fr.ses10doigts.toolkitbridge.service.tool.file.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class AgentFileController {

    private final AppendFileToolHandler appendFile;
    private final DeleteFileToolHandler deleteFile;
    private final ListFilesToolHandler listFiles;
    private final MoveFileToolHandler moveFile;
    private final ReadFileToolHandler readFile;
    private final WriteFileToolHandler writeFile;

    @GetMapping("/list")
    public ToolExecutionResult listFiles(@RequestParam(required = false) String path)
            throws IOException {
        Map<String, Object> arguments = new HashMap<>();
        if (path != null && !path.isBlank()) {
            arguments.put("path", path);
        }
        return listFiles.execute(arguments);
    }

    @GetMapping("/read")
    public ToolExecutionResult readFile(@RequestParam String path)
            throws IOException {
        return readFile.execute(Map.of("path", path));
    }

    @PostMapping("/write")
    public ToolExecutionResult writeFile(@RequestParam String path, @RequestBody String content)
            throws IOException {
        return writeFile.execute(Map.of(
                "path", path,
                "content", content
        ));
    }

    @PostMapping("/append")
    public ToolExecutionResult appendFile(@RequestParam String path, @RequestBody String content)
            throws IOException {
        return appendFile.execute(Map.of(
                "path", path,
                "content", content
        ));
    }

    @DeleteMapping("/delete")
    public ToolExecutionResult deleteFile(@RequestParam String path)
            throws IOException {
        return deleteFile.execute(Map.of("path", path));
    }

    @PostMapping("/move")
    public ToolExecutionResult moveFile(@RequestParam String sourcePath, @RequestParam String targetPath)
            throws IOException {
        return moveFile.execute(Map.of(
                "source_path", sourcePath,
                "target_path", targetPath
        ));
    }
}

