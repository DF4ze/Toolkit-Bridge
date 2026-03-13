package fr.ses10doigts.toolkitbridge.controler;

import fr.ses10doigts.toolkitbridge.model.dto.web.FileContentResponse;
import fr.ses10doigts.toolkitbridge.model.dto.web.FileEntryResponse;
import fr.ses10doigts.toolkitbridge.model.dto.web.SimpleResponse;
import fr.ses10doigts.toolkitbridge.service.botservice.BotFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class BotFileController {

    private final BotFileService botFileService;

    @GetMapping("/list")
    public List<FileEntryResponse> listFiles(@RequestParam(required = false) String path)
            throws IOException {
        return botFileService.listFiles(path);
    }

    @GetMapping("/read")
    public FileContentResponse readFile(@RequestParam String path)
            throws IOException {
        return botFileService.readFile(path);
    }

    @PostMapping("/write")
    public SimpleResponse writeFile(@RequestParam String path, @RequestBody String content)
            throws IOException {
        return botFileService.writeFile(path, content);
    }

    @PostMapping("/append")
    public SimpleResponse appendFile(@RequestParam String path, @RequestBody String content)
            throws IOException {
        return botFileService.appendFile(path, content);
    }

    @DeleteMapping("/delete")
    public SimpleResponse deleteFile(@RequestParam String path)
            throws IOException {
        return botFileService.deleteFile(path);
    }

    @PostMapping("/move")
    public SimpleResponse moveFile(@RequestParam String sourcePath, @RequestParam String targetPath)
            throws IOException {
        return botFileService.moveFile(sourcePath, targetPath);
    }
}

