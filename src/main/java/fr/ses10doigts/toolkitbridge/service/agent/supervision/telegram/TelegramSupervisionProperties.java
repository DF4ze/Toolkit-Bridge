package fr.ses10doigts.toolkitbridge.service.agent.supervision.telegram;

import fr.ses10doigts.toolkitbridge.service.agent.trace.model.AgentTraceEventType;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.EnumSet;
import java.util.Set;

@ConfigurationProperties(prefix = "toolkit.telegram.supervision")
public class TelegramSupervisionProperties {

    private boolean enabled = false;
    private String botId;
    private Long chatId;
    private boolean readOnly = true;
    private int maxMessageLength = 700;
    private Set<AgentTraceEventType> publishedTraceEventTypes = EnumSet.of(
            AgentTraceEventType.DELEGATION,
            AgentTraceEventType.RESPONSE,
            AgentTraceEventType.ERROR
    );
    private final HumanIntervention humanIntervention = new HumanIntervention();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getBotId() {
        return botId;
    }

    public void setBotId(String botId) {
        this.botId = blankToNull(botId);
    }

    public Long getChatId() {
        return chatId;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
    }

    public int getMaxMessageLength() {
        return maxMessageLength;
    }

    public void setMaxMessageLength(int maxMessageLength) {
        this.maxMessageLength = maxMessageLength;
    }

    public Set<AgentTraceEventType> getPublishedTraceEventTypes() {
        return publishedTraceEventTypes;
    }

    public void setPublishedTraceEventTypes(Set<AgentTraceEventType> publishedTraceEventTypes) {
        if (publishedTraceEventTypes == null || publishedTraceEventTypes.isEmpty()) {
            this.publishedTraceEventTypes = EnumSet.noneOf(AgentTraceEventType.class);
            return;
        }
        this.publishedTraceEventTypes = EnumSet.copyOf(publishedTraceEventTypes);
    }

    public HumanIntervention getHumanIntervention() {
        return humanIntervention;
    }

    public boolean hasTargetChat() {
        return enabled && chatId != null;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    public static class HumanIntervention {
        private boolean enabled = true;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }
    }
}
