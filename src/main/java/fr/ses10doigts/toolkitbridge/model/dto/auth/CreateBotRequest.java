package fr.ses10doigts.toolkitbridge.model.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateBotRequest {

    @NotBlank
    @Size(max = 100)
    @Pattern(regexp = "[a-zA-Z0-9._-]+")
    private String botIdent;
}