package fr.ses10doigts.toolkitbridge.controler.telegram.workflow;

import fr.ses10doigts.telegrambots.model.TelegramUpdateContext;
import fr.ses10doigts.telegrambots.service.poller.handler.annot.Command;
import fr.ses10doigts.telegrambots.service.poller.handler.annot.TelegramController;
import fr.ses10doigts.toolkitbridge.service.telegram.workflow.WorkflowTelegramOrchestrationService;
import fr.ses10doigts.toolkitbridge.service.telegram.workflow.WorkflowTelegramProjectService;
import fr.ses10doigts.toolkitbridge.service.telegram.workflow.WorkflowTelegramRoadmapService;
import fr.ses10doigts.toolkitbridge.service.telegram.workflow.WorkflowTelegramSummaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@TelegramController(bot = "Cortex")
@RequiredArgsConstructor
@Slf4j
public class WorkflowTelegramController {

    private final WorkflowTelegramRoadmapService roadmapService;
    private final WorkflowTelegramOrchestrationService orchestrationService;
    private final WorkflowTelegramSummaryService summaryService;
    private final WorkflowTelegramProjectService projectService;

    @Command(value = "/workflow", description = "Workflow interface")
    public String workflow(TelegramUpdateContext ctx) {
        log.debug("Handling workflow command chatId={} userId={} botId={}",
                ctx == null ? null : ctx.getChatId(),
                ctx == null ? null : ctx.getUserId(),
                ctx == null ? null : ctx.getBotId());
        return orchestrationService.workflowHome(ctx == null ? null : ctx.getChatId());
    }

    @Command(value = "/workflow_roadmap_load", description = "Load workflow roadmap")
    public String workflowRoadmapLoad(TelegramUpdateContext ctx) {
        String projectName = argValue(ctx, "project");
        if (projectName == null) {
            projectName = argValue(ctx, "projectName");
        }
        String path = argValue(ctx, "path");
        log.info("Command /workflow_roadmap_load: chatId={}, userId={}, project={}",
                ctx == null ? null : ctx.getChatId(),
                ctx == null ? null : ctx.getUserId(),
                projectName);
        return roadmapService.loadRoadmap(
                ctx == null ? null : ctx.getChatId(),
                ctx == null ? null : ctx.getUserId(),
                projectName,
                path
        );
    }

    @Command(value = "/workflow_run", description = "Run workflow")
    public String workflowRun(TelegramUpdateContext ctx) {
        String projectName = argValue(ctx, "project");
        if (projectName == null) {
            projectName = argValue(ctx, "projectName");
        }

        Integer phase = argIntValue(ctx, "phase");
        Integer etape = argIntValue(ctx, "etape");
        if (etape == null) {
            etape = argIntValue(ctx, "step");
        }

        log.info("Command /workflow_run: chatId={}, userId={}, project={}, phase={}, etape={}",
                ctx == null ? null : ctx.getChatId(),
                ctx == null ? null : ctx.getUserId(),
                projectName, phase, etape);
        return orchestrationService.startRun(
                ctx == null ? null : ctx.getChatId(),
                ctx == null ? null : ctx.getUserId(),
                ctx == null ? null : ctx.getBotId(),
                projectName,
                phase,
                etape
        );
    }

    @Command(value = "/workflow_status", description = "Workflow status")
    public String workflowStatus(TelegramUpdateContext ctx) {
        return orchestrationService.status(ctx == null ? null : ctx.getChatId());
    }

    @Command(value = "/workflow_resume", description = "Resume workflow after WAIT_HUMAN")
    public String workflowResume(TelegramUpdateContext ctx) {
        log.info("Command /workflow_resume: chatId={}, userId={}",
                ctx == null ? null : ctx.getChatId(),
                ctx == null ? null : ctx.getUserId());
        return orchestrationService.resume(
                ctx == null ? null : ctx.getChatId(),
                ctx == null ? null : ctx.getUserId()
        );
    }

    @Command(value = "/workflow_summary", description = "Workflow summary")
    public String workflowSummary(TelegramUpdateContext ctx) {
        return summaryService.workflowSummary(ctx);
    }

    @Command(value = "/workflow_project_set", description = "Register or update a workflow project path")
    public String workflowProjectSet(TelegramUpdateContext ctx) {
        log.info("Command /workflow_project_set: chatId={}, userId={}",
                ctx == null ? null : ctx.getChatId(),
                ctx == null ? null : ctx.getUserId());
        return projectService.setProject(ctx == null ? null : ctx.getArgs());
    }

    private String argValue(TelegramUpdateContext ctx, String key) {
        if (ctx == null || key == null || key.isBlank()) {
            return null;
        }
        if (ctx.getArgs() == null || ctx.getArgs().isEmpty()) {
            return null;
        }
        String prefix = key + "=";
        for (String arg : ctx.getArgs()) {
            if (arg == null) {
                continue;
            }
            String trimmed = arg.trim();
            if (trimmed.startsWith(prefix)) {
                String value = trimmed.substring(prefix.length()).trim();
                return value.isBlank() ? null : value;
            }
        }
        return null;
    }

    private Integer argIntValue(TelegramUpdateContext ctx, String key) {
        String value = argValue(ctx, key);
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
