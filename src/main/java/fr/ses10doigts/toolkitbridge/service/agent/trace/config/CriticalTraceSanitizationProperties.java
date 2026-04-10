package fr.ses10doigts.toolkitbridge.service.agent.trace.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "toolkit.observability.agent-tracing.critical-sanitization")
public class CriticalTraceSanitizationProperties {

    private int maxGenericText = 1000;
    private int maxErrorReasonText = 500;
    private int maxToolMessageText = 220;
    private int maxCollectionItems = 30;
    private int maxMapItems = 40;
    private int maxDepth = 2;

    public int getMaxGenericText() {
        return maxGenericText;
    }

    public void setMaxGenericText(int maxGenericText) {
        this.maxGenericText = maxGenericText;
    }

    public int getMaxErrorReasonText() {
        return maxErrorReasonText;
    }

    public void setMaxErrorReasonText(int maxErrorReasonText) {
        this.maxErrorReasonText = maxErrorReasonText;
    }

    public int getMaxToolMessageText() {
        return maxToolMessageText;
    }

    public void setMaxToolMessageText(int maxToolMessageText) {
        this.maxToolMessageText = maxToolMessageText;
    }

    public int getMaxCollectionItems() {
        return maxCollectionItems;
    }

    public void setMaxCollectionItems(int maxCollectionItems) {
        this.maxCollectionItems = maxCollectionItems;
    }

    public int getMaxMapItems() {
        return maxMapItems;
    }

    public void setMaxMapItems(int maxMapItems) {
        this.maxMapItems = maxMapItems;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }
}
