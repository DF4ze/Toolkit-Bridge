package fr.ses10doigts.toolkitbridge.memory.config.runtime;

import fr.ses10doigts.toolkitbridge.service.configuration.admin.payload.MemoryConfigurationPayload;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class MemoryRuntimeConfigurationMapper {

    /**
     * Maps the administrable payload to the runtime memory snapshot.
     * Runtime applies only fields that are currently wired in memory services.
     * In integration section, sizing fields (maxRules, maxSemanticMemories, maxEpisodes,
     * maxConversationMessages, maxContextCharacters) are intentionally ignored for now.
     */
    public MemoryRuntimeConfiguration toRuntimeConfiguration(MemoryConfigurationPayload payload) {
        MemoryRuntimeConfiguration defaults = MemoryRuntimeConfiguration.defaults();
        if (payload == null) {
            return defaults;
        }

        MemoryConfigurationPayload.Context payloadContext = payload.getContext();
        MemoryConfigurationPayload.Retrieval payloadRetrieval = payload.getRetrieval();
        MemoryConfigurationPayload.Integration payloadIntegration = payload.getIntegration();
        MemoryConfigurationPayload.Scoring payloadScoring = payload.getScoring();
        MemoryConfigurationPayload.GlobalContext payloadGlobalContext = payload.getGlobalContext();

        return new MemoryRuntimeConfiguration(
                new MemoryRuntimeConfiguration.Context(
                        positiveInt(payloadContext == null ? null : payloadContext.getMaxRules(), defaults.context().maxRules()),
                        positiveInt(payloadContext == null ? null : payloadContext.getMaxMemories(), defaults.context().maxMemories()),
                        positiveInt(payloadContext == null ? null : payloadContext.getMaxCharacters(), defaults.context().maxCharacters()),
                        positiveInt(payloadContext == null ? null : payloadContext.getMaxEpisodes(), defaults.context().maxEpisodes())
                ),
                new MemoryRuntimeConfiguration.Retrieval(
                        positiveInt(payloadRetrieval == null ? null : payloadRetrieval.getMaxRules(), defaults.retrieval().maxRules()),
                        positiveInt(payloadRetrieval == null ? null : payloadRetrieval.getMaxSemanticMemories(), defaults.retrieval().maxSemanticMemories()),
                        positiveInt(payloadRetrieval == null ? null : payloadRetrieval.getMaxCandidatePoolSize(), defaults.retrieval().maxCandidatePoolSize()),
                        positiveInt(payloadRetrieval == null ? null : payloadRetrieval.getMaxEpisodes(), defaults.retrieval().maxEpisodes()),
                        positiveInt(payloadRetrieval == null ? null : payloadRetrieval.getMaxProjectEpisodeFetch(), defaults.retrieval().maxProjectEpisodeFetch()),
                        positiveInt(payloadRetrieval == null ? null : payloadRetrieval.getConversationSliceMaxCharacters(), defaults.retrieval().conversationSliceMaxCharacters())
                ),
                new MemoryRuntimeConfiguration.Integration(
                        bool(payloadIntegration == null ? null : payloadIntegration.getEnableSemanticExtraction(), defaults.integration().enableSemanticExtraction()),
                        bool(payloadIntegration == null ? null : payloadIntegration.getEnableRulePromotion(), defaults.integration().enableRulePromotion()),
                        bool(payloadIntegration == null ? null : payloadIntegration.getEnableEpisodicInjection(), defaults.integration().enableEpisodicInjection()),
                        bool(payloadIntegration == null ? null : payloadIntegration.getMarkUsedEnabled(), defaults.integration().markUsedEnabled())
                ),
                new MemoryRuntimeConfiguration.Scoring(
                        finiteDouble(payloadScoring == null ? null : payloadScoring.getImportanceWeight(), defaults.scoring().importanceWeight()),
                        finiteDouble(payloadScoring == null ? null : payloadScoring.getUsageWeight(), defaults.scoring().usageWeight()),
                        finiteDouble(payloadScoring == null ? null : payloadScoring.getRecencyWeight(), defaults.scoring().recencyWeight())
                ),
                new MemoryRuntimeConfiguration.GlobalContext(
                        bool(payloadGlobalContext == null ? null : payloadGlobalContext.getEnabled(), defaults.globalContext().enabled()),
                        loadMode(payloadGlobalContext == null ? null : payloadGlobalContext.getLoadMode(), defaults.globalContext().loadMode()),
                        positiveDuration(payloadGlobalContext == null ? null : payloadGlobalContext.getCacheRefreshInterval(), defaults.globalContext().cacheRefreshInterval()),
                        copyFiles(payloadGlobalContext == null ? null : payloadGlobalContext.getFiles())
                )
        );
    }

    private int positiveInt(Integer value, int fallback) {
        return value == null || value <= 0 ? fallback : value;
    }

    private boolean bool(Boolean value, boolean fallback) {
        return value == null ? fallback : value;
    }

    private double finiteDouble(Double value, double fallback) {
        if (value == null || Double.isNaN(value) || Double.isInfinite(value)) {
            return fallback;
        }
        return value;
    }

    private Duration positiveDuration(Duration value, Duration fallback) {
        if (value == null || value.isNegative() || value.isZero()) {
            return fallback;
        }
        return value;
    }

    private MemoryRuntimeConfiguration.GlobalContextLoadMode loadMode(
            String value,
            MemoryRuntimeConfiguration.GlobalContextLoadMode fallback
    ) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        for (MemoryRuntimeConfiguration.GlobalContextLoadMode mode : MemoryRuntimeConfiguration.GlobalContextLoadMode.values()) {
            if (mode.name().equals(normalized)) {
                return mode;
            }
        }
        return fallback;
    }

    private List<String> copyFiles(List<String> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }
        List<String> copy = new ArrayList<>();
        for (String file : files) {
            if (file == null || file.isBlank()) {
                continue;
            }
            copy.add(file.trim());
        }
        return List.copyOf(copy);
    }
}
