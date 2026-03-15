package fr.ses10doigts.toolkitbridge.model.dto.tool.file;


import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Builder
@Getter
public class FileResponse{
    private String path;

    private FileContentResponse content;
    private List<FileEntryResponse> entry;
}
