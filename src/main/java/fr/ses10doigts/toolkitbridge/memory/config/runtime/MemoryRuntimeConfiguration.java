package fr.ses10doigts.toolkitbridge.memory.config.runtime;

import java.time.Duration;
import java.util.List;

public record MemoryRuntimeConfiguration(
        Context context,
        Retrieval retrieval,
        Integration integration,
        Scoring scoring,
        GlobalContext globalContext
) {

    public static MemoryRuntimeConfiguration defaults() {
        return MemoryConfigurationDefaults.runtimeConfiguration();
    }

    public record Context(
            int maxRules,
            int maxMemories,
            int maxCharacters,
            int maxEpisodes
    ) {
    }

    public record Retrieval(
            int maxRules,
            int maxSemanticMemories,
            int maxCandidatePoolSize,
            int maxEpisodes,
            int maxProjectEpisodeFetch,
            int conversationSliceMaxCharacters
    ) {
    }

    public record Integration(
            boolean enableSemanticExtraction,
            boolean enableRulePromotion,
            boolean enableEpisodicInjection,
            boolean markUsedEnabled
    ) {
    }

    public record Scoring(
            double importanceWeight,
            double usageWeight,
            double recencyWeight
    ) {
    }

    public record GlobalContext(
            boolean enabled,
            GlobalContextLoadMode loadMode,
            Duration cacheRefreshInterval,
            List<String> files
    ) {
    }

    public enum GlobalContextLoadMode {
        ON_DEMAND,
        CACHED
    }
}
