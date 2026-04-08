package fr.ses10doigts.toolkitbridge.model.dto.admin.telegram;

public record TelegramBotAdminResponse(
        String botId,
        boolean supervisionEnabled,
        Long supervisionChatId,
        boolean readOnly,
        boolean humanInterventionEnabled
) {
}
