package fr.ses10doigts.toolkitbridge.service.configuration.admin.payload;

import lombok.Data;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Data
public class MemoryConfigurationPayload {

    private Conversation conversation = new Conversation();
    private Context context = new Context();
    private Retrieval retrieval = new Retrieval();
    private Integration integration = new Integration();
    private Scoring scoring = new Scoring();
    private GlobalContext globalContext = new GlobalContext();

    @Data
    public static class Conversation {
        private Boolean enabled;
        private Integer maxRecentMessages;
        private Integer maxRecentCharacters;
        private Integer minMessagesToKeep;
        private Long expireAfterMinutes;
        private Boolean autoSummarize;
        private Integer maxSummaryCharacters;
    }

    @Data
    public static class Context {
        private Integer maxRules;
        private Integer maxMemories;
        private Integer maxCharacters;
        private Integer maxEpisodes;
    }

    @Data
    public static class Retrieval {
        private Integer maxRules;
        private Integer maxSemanticMemories;
        private Integer maxCandidatePoolSize;
        private Integer maxEpisodes;
        private Integer maxProjectEpisodeFetch;
        private Integer conversationSliceMaxCharacters;
    }

    @Data
    public static class Integration {
        private Boolean enableSemanticExtraction;
        private Boolean enableRulePromotion;
        private Boolean enableEpisodicInjection;
        private Boolean markUsedEnabled;
        private Integer maxRules;
        private Integer maxSemanticMemories;
        private Integer maxEpisodes;
        private Integer maxConversationMessages;
        private Integer maxContextCharacters;
    }

    @Data
    public static class Scoring {
        private Double importanceWeight;
        private Double usageWeight;
        private Double recencyWeight;
    }

    @Data
    public static class GlobalContext {
        private Boolean enabled;
        private String loadMode;
        private Duration cacheRefreshInterval;
        private List<String> files = new ArrayList<>();
    }
}
