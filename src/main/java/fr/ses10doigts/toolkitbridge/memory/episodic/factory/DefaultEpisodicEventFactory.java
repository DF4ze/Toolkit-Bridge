package fr.ses10doigts.toolkitbridge.memory.episodic.factory;

import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeEvent;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeEventType;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeScope;
import fr.ses10doigts.toolkitbridge.memory.episodic.model.EpisodeStatus;
import fr.ses10doigts.toolkitbridge.memory.facade.model.MemoryContextRequest;
import fr.ses10doigts.toolkitbridge.memory.facade.model.ToolExecutionRecord;
import org.springframework.stereotype.Component;

@Component
public class DefaultEpisodicEventFactory implements EpisodicEventFactory {

    @Override
    public EpisodeEvent userMessageReceived(MemoryContextRequest request) {
        EpisodeEvent event = baseEvent(request);
        event.setType(EpisodeEventType.ACTION);
        event.setStatus(EpisodeStatus.SUCCESS);
        event.setAction("user_message_received");
        event.setDetails(shorten(request.currentUserMessage()));
        return event;
    }

    @Override
    public EpisodeEvent assistantResponseGenerated(MemoryContextRequest request, String assistantMessage) {
        EpisodeEvent event = baseEvent(request);
        event.setType(EpisodeEventType.RESULT);
        event.setStatus(EpisodeStatus.SUCCESS);
        event.setAction("assistant_response_generated");
        event.setDetails("response_length=" + (assistantMessage == null ? 0 : assistantMessage.length()));
        return event;
    }

    @Override
    public EpisodeEvent toolExecutionEvent(MemoryContextRequest request, ToolExecutionRecord toolExecutionRecord) {
        EpisodeEvent event = baseEvent(request);
        event.setType(toolExecutionRecord.success() ? EpisodeEventType.ACTION : EpisodeEventType.ERROR);
        event.setStatus(toolExecutionRecord.success() ? EpisodeStatus.SUCCESS : EpisodeStatus.FAILURE);
        event.setAction(toolExecutionRecord.success() ? "tool_execution_success" : "tool_execution_failure");
        event.setDetails("tool=" + toolExecutionRecord.toolName() + "; " + shorten(toolExecutionRecord.details()));
        return event;
    }

    @Override
    public EpisodeEvent orchestrationFallback(MemoryContextRequest request, String reason) {
        EpisodeEvent event = baseEvent(request);
        event.setType(EpisodeEventType.ERROR);
        event.setStatus(EpisodeStatus.FAILURE);
        event.setAction("orchestration_fallback");
        event.setDetails(shorten(reason));
        return event;
    }

    private EpisodeEvent baseEvent(MemoryContextRequest request) {
        EpisodeEvent event = new EpisodeEvent();
        event.setAgentId(request.agentId());
        event.setScope(EpisodeScope.AGENT);
        event.setScopeId(request.conversationId());
        return event;
    }

    private String shorten(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String trimmed = value.trim();
        if (trimmed.length() <= 400) {
            return trimmed;
        }
        return trimmed.substring(0, 397) + "...";
    }
}
