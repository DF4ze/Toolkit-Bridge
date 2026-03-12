package fr.ses10doigts.toolkitbridge.model;

import lombok.Data;

import java.util.List;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

@Data
public class CommandRequest {

    @NotBlank
    private String tool;

    private List<String> args;

    @Min(1)
    @Max(60)
    private Integer timeout = 10;
}