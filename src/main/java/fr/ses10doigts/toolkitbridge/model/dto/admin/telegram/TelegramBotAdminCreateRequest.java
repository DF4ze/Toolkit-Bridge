package fr.ses10doigts.toolkitbridge.model.dto.admin.telegram;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TelegramBotAdminCreateRequest(
        @NotBlank
        @Size(max = 100)
        @Pattern(regexp = "[a-zA-Z0-9._-]+")
        String botId
) {
}
