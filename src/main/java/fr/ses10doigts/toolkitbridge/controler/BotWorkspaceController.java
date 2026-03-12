package fr.ses10doigts.toolkitbridge.controler;

import fr.ses10doigts.toolkitbridge.model.FileContentResponse;
import fr.ses10doigts.toolkitbridge.model.FileEntryResponse;
import fr.ses10doigts.toolkitbridge.model.SimpleResponse;
import fr.ses10doigts.toolkitbridge.service.BotWorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class BotWorkspaceController {

    private final BotWorkspaceService botWorkspaceService;

    @GetMapping("/list")
    public List<FileEntryResponse> listFiles(@RequestParam(required = false) String path)
            throws IOException {
        return botWorkspaceService.listFiles(path);
    }

    @GetMapping("/read")
    public FileContentResponse readFile(@RequestParam String path)
            throws IOException {
        return botWorkspaceService.readFile(path);
    }

    @PostMapping("/write")
    public SimpleResponse writeFile(@RequestParam String path, @RequestBody String content)
            throws IOException {
        return botWorkspaceService.writeFile(path, content);
    }

    @PostMapping("/append")
    public SimpleResponse appendFile(@RequestParam String path, @RequestBody String content)
            throws IOException {
        return botWorkspaceService.appendFile(path, content);
    }

    @DeleteMapping("/delete")
    public SimpleResponse deleteFile(@RequestParam String path)
            throws IOException {
        return botWorkspaceService.deleteFile(path);
    }

    @PostMapping("/move")
    public SimpleResponse moveFile(@RequestParam String sourcePath, @RequestParam String targetPath)
            throws IOException {
        return botWorkspaceService.moveFile(sourcePath, targetPath);
    }
}

