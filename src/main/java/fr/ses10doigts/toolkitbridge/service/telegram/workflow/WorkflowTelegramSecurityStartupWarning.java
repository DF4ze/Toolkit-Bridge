package fr.ses10doigts.toolkitbridge.service.telegram.workflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class WorkflowTelegramSecurityStartupWarning implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WorkflowTelegramSecurityStartupWarning.class);

    private static final String WORKFLOW_BOT_ID = "Cortex";

    private final Environment environment;

    public WorkflowTelegramSecurityStartupWarning(Environment environment) {
        this.environment = Objects.requireNonNull(environment, "environment must not be null");
    }

    @Override
    public void run(ApplicationArguments args) {
        Boolean telegramEnabled = environment.getProperty("telegram.enabled", Boolean.class);
        if (telegramEnabled != null && !telegramEnabled) {
            return;
        }

        List<TelegramBotConfig> bots = Binder.get(environment)
                .bind("telegram.bots", Bindable.listOf(TelegramBotConfig.class))
                .orElse(List.of());

        TelegramBotConfig cortex = bots.stream()
                .filter(bot -> WORKFLOW_BOT_ID.equals(bot.id()))
                .findFirst()
                .orElse(null);
        if (cortex == null) {
            return;
        }

        List<Long> allowedUserIds = cortex.security() == null ? null : cortex.security().allowedUserIds();
        if (allowedUserIds == null || allowedUserIds.isEmpty()) {
            log.warn(
                    "Telegram workflow bot {} has an empty whitelist. Workflow commands may be accessible to any Telegram user reaching this bot.",
                    WORKFLOW_BOT_ID
            );
        }
    }

    record TelegramBotConfig(String id, TelegramSecurityConfig security) {
    }

    record TelegramSecurityConfig(List<Long> allowedUserIds) {
    }
}

