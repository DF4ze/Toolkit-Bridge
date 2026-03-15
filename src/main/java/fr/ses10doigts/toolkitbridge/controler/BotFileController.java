package fr.ses10doigts.toolkitbridge.controler;

import fr.ses10doigts.toolkitbridge.model.dto.tool.ToolExecutionResult;
import fr.ses10doigts.toolkitbridge.service.tool.file.BotFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class BotFileController {

    private final BotFileService botFileService;

    @GetMapping("/list")
    public ToolExecutionResult listFiles(@RequestParam(required = false) String path)
            throws IOException {
        return botFileService.listFiles(path);
    }

    @GetMapping("/read")
    public ToolExecutionResult readFile(@RequestParam String path)
            throws IOException {
        return botFileService.readFile(path);
    }

    @PostMapping("/write")
    public ToolExecutionResult writeFile(@RequestParam String path, @RequestBody String content)
            throws IOException {
        return botFileService.writeFile(path, content);
    }

    @PostMapping("/append")
    public ToolExecutionResult appendFile(@RequestParam String path, @RequestBody String content)
            throws IOException {
        return botFileService.appendFile(path, content);
    }

    @DeleteMapping("/delete")
    public ToolExecutionResult deleteFile(@RequestParam String path)
            throws IOException {
        return botFileService.deleteFile(path);
    }

    @PostMapping("/move")
    public ToolExecutionResult moveFile(@RequestParam String sourcePath, @RequestParam String targetPath)
            throws IOException {
        return botFileService.moveFile(sourcePath, targetPath);
    }
}

