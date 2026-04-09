package fr.ses10doigts.toolkitbridge.memory.config.runtime;

import java.time.Duration;
import java.util.List;

public final class MemoryConfigurationDefaults {

    public static final int CONTEXT_MAX_RULES = 10;
    public static final int CONTEXT_MAX_MEMORIES = 10;
    public static final int CONTEXT_MAX_CHARACTERS = 15000;
    public static final int CONTEXT_MAX_EPISODES = 5;

    public static final int RETRIEVAL_MAX_RULES = 10;
    public static final int RETRIEVAL_MAX_SEMANTIC_MEMORIES = 10;
    public static final int RETRIEVAL_MAX_CANDIDATE_POOL_SIZE = 25;
    public static final int RETRIEVAL_MAX_EPISODES = 5;
    public static final int RETRIEVAL_MAX_PROJECT_EPISODE_FETCH = 5;
    public static final int RETRIEVAL_CONVERSATION_SLICE_MAX_CHARACTERS = 4000;

    public static final boolean INTEGRATION_ENABLE_SEMANTIC_EXTRACTION = true;
    public static final boolean INTEGRATION_ENABLE_RULE_PROMOTION = true;
    public static final boolean INTEGRATION_ENABLE_EPISODIC_INJECTION = true;
    public static final boolean INTEGRATION_MARK_USED_ENABLED = true;
    public static final int INTEGRATION_MAX_RULES = 10;
    public static final int INTEGRATION_MAX_SEMANTIC_MEMORIES = 10;
    public static final int INTEGRATION_MAX_EPISODES = 5;
    public static final int INTEGRATION_MAX_CONVERSATION_MESSAGES = 20;
    public static final int INTEGRATION_MAX_CONTEXT_CHARACTERS = 15000;

    public static final double SCORING_IMPORTANCE_WEIGHT = 1.0;
    public static final double SCORING_USAGE_WEIGHT = 0.5;
    public static final double SCORING_RECENCY_WEIGHT = 1.0;

    public static final boolean GLOBAL_CONTEXT_ENABLED = true;
    public static final MemoryRuntimeConfiguration.GlobalContextLoadMode GLOBAL_CONTEXT_LOAD_MODE =
            MemoryRuntimeConfiguration.GlobalContextLoadMode.ON_DEMAND;
    public static final Duration GLOBAL_CONTEXT_CACHE_REFRESH_INTERVAL = Duration.ofSeconds(30);
    public static final List<String> GLOBAL_CONTEXT_FILES = List.of();

    private MemoryConfigurationDefaults() {
    }

    public static MemoryRuntimeConfiguration runtimeConfiguration() {
        return new MemoryRuntimeConfiguration(
                new MemoryRuntimeConfiguration.Context(
                        CONTEXT_MAX_RULES,
                        CONTEXT_MAX_MEMORIES,
                        CONTEXT_MAX_CHARACTERS,
                        CONTEXT_MAX_EPISODES
                ),
                new MemoryRuntimeConfiguration.Retrieval(
                        RETRIEVAL_MAX_RULES,
                        RETRIEVAL_MAX_SEMANTIC_MEMORIES,
                        RETRIEVAL_MAX_CANDIDATE_POOL_SIZE,
                        RETRIEVAL_MAX_EPISODES,
                        RETRIEVAL_MAX_PROJECT_EPISODE_FETCH,
                        RETRIEVAL_CONVERSATION_SLICE_MAX_CHARACTERS
                ),
                new MemoryRuntimeConfiguration.Integration(
                        INTEGRATION_ENABLE_SEMANTIC_EXTRACTION,
                        INTEGRATION_ENABLE_RULE_PROMOTION,
                        INTEGRATION_ENABLE_EPISODIC_INJECTION,
                        INTEGRATION_MARK_USED_ENABLED
                ),
                new MemoryRuntimeConfiguration.Scoring(
                        SCORING_IMPORTANCE_WEIGHT,
                        SCORING_USAGE_WEIGHT,
                        SCORING_RECENCY_WEIGHT
                ),
                new MemoryRuntimeConfiguration.GlobalContext(
                        GLOBAL_CONTEXT_ENABLED,
                        GLOBAL_CONTEXT_LOAD_MODE,
                        GLOBAL_CONTEXT_CACHE_REFRESH_INTERVAL,
                        GLOBAL_CONTEXT_FILES
                )
        );
    }
}
