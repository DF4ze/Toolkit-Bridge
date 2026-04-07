package fr.ses10doigts.toolkitbridge.service.agent.supervision.telegram;

import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEvent;
import fr.ses10doigts.toolkitbridge.service.agent.trace.sink.AgentTraceSink;
import org.springframework.stereotype.Component;

@Component
public class TelegramAgentTraceSink implements AgentTraceSink {

    private final TelegramSupervisionProperties properties;
    private final TelegramTraceMessageFormatter formatter;
    private final TelegramSupervisionMessagePublisher publisher;

    public TelegramAgentTraceSink(TelegramSupervisionProperties properties,
                                  TelegramTraceMessageFormatter formatter,
                                  TelegramSupervisionMessagePublisher publisher) {
        this.properties = properties;
        this.formatter = formatter;
        this.publisher = publisher;
    }

    @Override
    public void publish(AgentTraceEvent event) {
        if (!shouldPublish(event)) {
            return;
        }

        formatter.format(event)
                .ifPresent(publisher::publish);
    }

    private boolean shouldPublish(AgentTraceEvent event) {
        return event != null
                && publisher.isConfigured()
                && event.type() != null
                && properties.getPublishedTraceEventTypes().contains(event.type());
    }
}
