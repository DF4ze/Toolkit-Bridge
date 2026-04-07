package fr.ses10doigts.toolkitbridge.model.dto.tool;

import fr.ses10doigts.toolkitbridge.model.dto.tool.bash.BashResponse;
import fr.ses10doigts.toolkitbridge.model.dto.tool.file.FileResponse;
import fr.ses10doigts.toolkitbridge.model.dto.tool.memory.MemoryResponse;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ToolExecutionResult {
    private boolean error;
    private String message;

    private FileResponse file;
    private BashResponse bash;
    private MemoryResponse memory;
}
