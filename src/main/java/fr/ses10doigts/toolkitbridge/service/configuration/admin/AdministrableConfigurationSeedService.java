package fr.ses10doigts.toolkitbridge.service.configuration.admin;

import fr.ses10doigts.toolkitbridge.config.llm.OpenAiLikeProperties;
import fr.ses10doigts.toolkitbridge.config.llm.OpenAiLikeProvidersProperties;
import fr.ses10doigts.toolkitbridge.memory.context.config.ContextAssemblerProperties;
import fr.ses10doigts.toolkitbridge.memory.context.global.config.GlobalContextProperties;
import fr.ses10doigts.toolkitbridge.memory.conversation.config.ConversationMemoryProperties;
import fr.ses10doigts.toolkitbridge.memory.integration.config.MemoryIntegrationProperties;
import fr.ses10doigts.toolkitbridge.memory.retrieval.config.MemoryRetrievalProperties;
import fr.ses10doigts.toolkitbridge.memory.scoring.config.MemoryScoringProperties;
import fr.ses10doigts.toolkitbridge.persistence.retention.PersistenceRetentionProperties;
import fr.ses10doigts.toolkitbridge.service.agent.artifact.config.ArtifactStorageProperties;
import fr.ses10doigts.toolkitbridge.service.configuration.admin.payload.ArtifactsConfigurationPayload;
import fr.ses10doigts.toolkitbridge.service.configuration.admin.payload.MemoryConfigurationPayload;
import fr.ses10doigts.toolkitbridge.service.configuration.admin.payload.RetentionConfigurationPayload;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.type.TypeReference;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@Slf4j
public class AdministrableConfigurationSeedService {

    private static final TypeReference<List<OpenAiLikeProperties>> OPENAI_LIKE_PROVIDERS_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<ArtifactsConfigurationPayload> ARTIFACTS_CONFIGURATION_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<RetentionConfigurationPayload> RETENTION_CONFIGURATION_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<MemoryConfigurationPayload> MEMORY_CONFIGURATION_TYPE = new TypeReference<>() {
    };

    private final AdministrableConfigurationStoreService storeService;
    private final OpenAiLikeProvidersProperties openAiLikeProvidersProperties;
    private final ArtifactStorageProperties artifactStorageProperties;
    private final PersistenceRetentionProperties retentionProperties;
    private final ConversationMemoryProperties conversationMemoryProperties;
    private final ContextAssemblerProperties contextAssemblerProperties;
    private final MemoryRetrievalProperties memoryRetrievalProperties;
    private final MemoryIntegrationProperties memoryIntegrationProperties;
    private final MemoryScoringProperties memoryScoringProperties;
    private final GlobalContextProperties globalContextProperties;

    public AdministrableConfigurationSeedService(
            AdministrableConfigurationStoreService storeService,
            OpenAiLikeProvidersProperties openAiLikeProvidersProperties,
            ArtifactStorageProperties artifactStorageProperties,
            PersistenceRetentionProperties retentionProperties,
            ConversationMemoryProperties conversationMemoryProperties,
            ContextAssemblerProperties contextAssemblerProperties,
            MemoryRetrievalProperties memoryRetrievalProperties,
            MemoryIntegrationProperties memoryIntegrationProperties,
            MemoryScoringProperties memoryScoringProperties,
            GlobalContextProperties globalContextProperties
    ) {
        this.storeService = storeService;
        this.openAiLikeProvidersProperties = openAiLikeProvidersProperties;
        this.artifactStorageProperties = artifactStorageProperties;
        this.retentionProperties = retentionProperties;
        this.conversationMemoryProperties = conversationMemoryProperties;
        this.contextAssemblerProperties = contextAssemblerProperties;
        this.memoryRetrievalProperties = memoryRetrievalProperties;
        this.memoryIntegrationProperties = memoryIntegrationProperties;
        this.memoryScoringProperties = memoryScoringProperties;
        this.globalContextProperties = globalContextProperties;
    }

    @Transactional
    public boolean bootstrapSeedsIfMissing() {
        boolean llmUpdated = bootstrapOpenAiLikeProviders();
        boolean artifactsUpdated = bootstrapArtifactsConfiguration();
        boolean retentionUpdated = bootstrapRetentionConfiguration();
        boolean memoryUpdated = bootstrapMemoryConfiguration();
        return llmUpdated || artifactsUpdated || retentionUpdated || memoryUpdated;
    }

    private boolean bootstrapOpenAiLikeProviders() {
        List<OpenAiLikeProperties> seedProviders = loadOpenAiLikeProvidersSeed();
        List<OpenAiLikeProperties> currentProviders = storeService.read(
                        AdministrableConfigKey.OPENAI_LIKE_PROVIDERS,
                        OPENAI_LIKE_PROVIDERS_TYPE
                )
                .orElse(List.of());

        Map<String, Integer> indexByName = new LinkedHashMap<>();
        List<OpenAiLikeProperties> mergedProviders = new java.util.ArrayList<>();
        for (OpenAiLikeProperties provider : currentProviders) {
            if (provider == null) {
                continue;
            }
            mergedProviders.add(provider);
            if (!isBlank(provider.name())) {
                indexByName.put(normalizeKey(provider.name()), mergedProviders.size() - 1);
            }
        }

        int createdProviders = 0;
        int completedFields = 0;
        for (OpenAiLikeProperties seedProvider : seedProviders) {
            if (seedProvider == null || isBlank(seedProvider.name())) {
                continue;
            }
            String key = normalizeKey(seedProvider.name());
            Integer existingIndex = indexByName.get(key);
            if (existingIndex == null) {
                mergedProviders.add(seedProvider);
                indexByName.put(key, mergedProviders.size() - 1);
                createdProviders++;
                log.info("BOOTSTRAP: missing -> created domain=llm.provider name={}", seedProvider.name());
                continue;
            }

            OpenAiLikeProperties existing = mergedProviders.get(existingIndex);
            OpenAiLikeProperties merged = mergeProvider(existing, seedProvider);
            if (!Objects.equals(existing, merged)) {
                mergedProviders.set(existingIndex, merged);
                int completed = countCompletedProviderFields(existing, merged);
                completedFields += completed;
                log.info("BOOTSTRAP: existing -> completed domain=llm.provider name={} completedFields={}",
                        safeProviderName(merged),
                        completed);
            } else {
                log.info("BOOTSTRAP: existing -> skipped domain=llm.provider name={}", safeProviderName(existing));
            }
        }

        boolean changed = !Objects.equals(currentProviders, mergedProviders);
        if (changed) {
            storeService.write(AdministrableConfigKey.OPENAI_LIKE_PROVIDERS, mergedProviders);
        }
        log.info("BOOTSTRAP: summary domain=llm.providers created={} completedFields={} changed={}",
                createdProviders,
                completedFields,
                changed);
        return changed;
    }

    private boolean bootstrapArtifactsConfiguration() {
        ArtifactsConfigurationPayload seed = toArtifactsPayload();
        Optional<ArtifactsConfigurationPayload> existingOptional = storeService.read(
                AdministrableConfigKey.ARTIFACTS_CONFIGURATION,
                ARTIFACTS_CONFIGURATION_TYPE
        );
        if (existingOptional.isEmpty()) {
            storeService.write(AdministrableConfigKey.ARTIFACTS_CONFIGURATION, seed);
            log.info("BOOTSTRAP: missing -> created domain=artifacts.configuration");
            return true;
        }

        ArtifactsConfigurationPayload existing = existingOptional.get();
        MergeStats stats = new MergeStats();
        ArtifactsConfigurationPayload merged = mergeArtifacts(existing, seed, stats);
        if (stats.changed()) {
            storeService.write(AdministrableConfigKey.ARTIFACTS_CONFIGURATION, merged);
            log.info("BOOTSTRAP: existing -> completed domain=artifacts.configuration completedFields={}", stats.completedFields());
            return true;
        }
        log.info("BOOTSTRAP: existing -> skipped domain=artifacts.configuration");
        return false;
    }

    private boolean bootstrapRetentionConfiguration() {
        RetentionConfigurationPayload seed = toRetentionPayload();
        Optional<RetentionConfigurationPayload> existingOptional = storeService.read(
                AdministrableConfigKey.RETENTION_CONFIGURATION,
                RETENTION_CONFIGURATION_TYPE
        );
        if (existingOptional.isEmpty()) {
            storeService.write(AdministrableConfigKey.RETENTION_CONFIGURATION, seed);
            log.info("BOOTSTRAP: missing -> created domain=retention.configuration");
            return true;
        }

        RetentionConfigurationPayload existing = existingOptional.get();
        MergeStats stats = new MergeStats();
        RetentionConfigurationPayload merged = mergeRetention(existing, seed, stats);
        if (stats.changed()) {
            storeService.write(AdministrableConfigKey.RETENTION_CONFIGURATION, merged);
            log.info("BOOTSTRAP: existing -> completed domain=retention.configuration completedFields={}", stats.completedFields());
            return true;
        }
        log.info("BOOTSTRAP: existing -> skipped domain=retention.configuration");
        return false;
    }

    private boolean bootstrapMemoryConfiguration() {
        MemoryConfigurationPayload seed = toMemoryPayload();
        Optional<MemoryConfigurationPayload> existingOptional = storeService.read(
                AdministrableConfigKey.MEMORY_CONFIGURATION,
                MEMORY_CONFIGURATION_TYPE
        );
        if (existingOptional.isEmpty()) {
            storeService.write(AdministrableConfigKey.MEMORY_CONFIGURATION, seed);
            log.info("BOOTSTRAP: missing -> created domain=memory.configuration");
            return true;
        }

        MemoryConfigurationPayload existing = existingOptional.get();
        MergeStats stats = new MergeStats();
        MemoryConfigurationPayload merged = mergeMemory(existing, seed, stats);
        if (stats.changed()) {
            storeService.write(AdministrableConfigKey.MEMORY_CONFIGURATION, merged);
            log.info("BOOTSTRAP: existing -> completed domain=memory.configuration completedFields={}", stats.completedFields());
            return true;
        }
        log.info("BOOTSTRAP: existing -> skipped domain=memory.configuration");
        return false;
    }

    private List<OpenAiLikeProperties> loadOpenAiLikeProvidersSeed() {
        List<OpenAiLikeProperties> providers = openAiLikeProvidersProperties.getProviders();
        return providers == null ? List.of() : List.copyOf(providers);
    }

    // Merge rule for providers:
    // - identity: normalized provider name
    // - existing non-blank fields are preserved
    // - only blank/missing fields are completed from seed
    // - secrets (apiKey) are merged with the same rule and never logged by value
    private OpenAiLikeProperties mergeProvider(OpenAiLikeProperties existing, OpenAiLikeProperties seed) {
        String name = coalesceNonBlank(existing.name(), seed.name());
        String baseUrl = coalesceNonBlank(existing.baseUrl(), seed.baseUrl());
        String apiKey = coalesceNonBlank(existing.apiKey(), seed.apiKey());
        String defaultModel = coalesceNonBlank(existing.defaultModel(), seed.defaultModel());
        return new OpenAiLikeProperties(name, baseUrl, apiKey, defaultModel);
    }

    private int countCompletedProviderFields(OpenAiLikeProperties before, OpenAiLikeProperties after) {
        int completed = 0;
        if (wasCompleted(before == null ? null : before.name(), after == null ? null : after.name())) {
            completed++;
        }
        if (wasCompleted(before == null ? null : before.baseUrl(), after == null ? null : after.baseUrl())) {
            completed++;
        }
        if (wasCompleted(before == null ? null : before.defaultModel(), after == null ? null : after.defaultModel())) {
            completed++;
        }
        if (wasCompleted(before == null ? null : before.apiKey(), after == null ? null : after.apiKey())) {
            completed++;
        }
        return completed;
    }

    private ArtifactsConfigurationPayload toArtifactsPayload() {
        ArtifactsConfigurationPayload payload = new ArtifactsConfigurationPayload();
        payload.setEnabled(artifactStorageProperties.isEnabled());
        payload.setContentFolder(artifactStorageProperties.getContentFolder());

        ArtifactsConfigurationPayload.Retention retention = new ArtifactsConfigurationPayload.Retention();
        retention.setDefaultTtl(artifactStorageProperties.getRetention().getDefaultTtl());
        retention.setByType(new LinkedHashMap<>(artifactStorageProperties.getRetention().getByType()));
        payload.setRetention(retention);
        return payload;
    }

    private RetentionConfigurationPayload toRetentionPayload() {
        RetentionConfigurationPayload payload = new RetentionConfigurationPayload();
        payload.setDefaultTtl(retentionProperties.getDefaultTtl());

        Map<String, RetentionConfigurationPayload.FamilyPolicyPayload> families = new LinkedHashMap<>();
        retentionProperties.getFamilies().forEach((familyKey, familyPolicy) -> {
            RetentionConfigurationPayload.FamilyPolicyPayload familyPayload = new RetentionConfigurationPayload.FamilyPolicyPayload();
            familyPayload.setTtl(familyPolicy.getTtl());
            familyPayload.setDisposition(familyPolicy.getDisposition());

            Map<String, RetentionConfigurationPayload.DomainPolicyPayload> domains = new LinkedHashMap<>();
            familyPolicy.getDomains().forEach((domainKey, domainPolicy) -> {
                RetentionConfigurationPayload.DomainPolicyPayload domainPayload = new RetentionConfigurationPayload.DomainPolicyPayload();
                domainPayload.setTtl(domainPolicy.getTtl());
                domainPayload.setDisposition(domainPolicy.getDisposition());
                domains.put(domainKey, domainPayload);
            });
            familyPayload.setDomains(domains);
            families.put(familyKey, familyPayload);
        });
        payload.setFamilies(families);
        return payload;
    }

    private MemoryConfigurationPayload toMemoryPayload() {
        MemoryConfigurationPayload payload = new MemoryConfigurationPayload();

        MemoryConfigurationPayload.Conversation conversation = new MemoryConfigurationPayload.Conversation();
        conversation.setEnabled(conversationMemoryProperties.isEnabled());
        conversation.setMaxRecentMessages(conversationMemoryProperties.getMaxRecentMessages());
        conversation.setMaxRecentCharacters(conversationMemoryProperties.getMaxRecentCharacters());
        conversation.setMinMessagesToKeep(conversationMemoryProperties.getMinMessagesToKeep());
        conversation.setExpireAfterMinutes(conversationMemoryProperties.getExpireAfterMinutes());
        conversation.setAutoSummarize(conversationMemoryProperties.isAutoSummarize());
        conversation.setMaxSummaryCharacters(conversationMemoryProperties.getMaxSummaryCharacters());
        payload.setConversation(conversation);

        MemoryConfigurationPayload.Context context = new MemoryConfigurationPayload.Context();
        context.setMaxRules(contextAssemblerProperties.getMaxRules());
        context.setMaxMemories(contextAssemblerProperties.getMaxMemories());
        context.setMaxCharacters(contextAssemblerProperties.getMaxCharacters());
        context.setMaxEpisodes(contextAssemblerProperties.getMaxEpisodes());
        payload.setContext(context);

        MemoryConfigurationPayload.Retrieval retrieval = new MemoryConfigurationPayload.Retrieval();
        retrieval.setMaxRules(memoryRetrievalProperties.getMaxRules());
        retrieval.setMaxSemanticMemories(memoryRetrievalProperties.getMaxSemanticMemories());
        retrieval.setMaxCandidatePoolSize(memoryRetrievalProperties.getMaxCandidatePoolSize());
        retrieval.setMaxEpisodes(memoryRetrievalProperties.getMaxEpisodes());
        retrieval.setMaxProjectEpisodeFetch(memoryRetrievalProperties.getMaxProjectEpisodeFetch());
        retrieval.setConversationSliceMaxCharacters(memoryRetrievalProperties.getConversationSliceMaxCharacters());
        payload.setRetrieval(retrieval);

        MemoryConfigurationPayload.Integration integration = new MemoryConfigurationPayload.Integration();
        integration.setEnableSemanticExtraction(memoryIntegrationProperties.isEnableSemanticExtraction());
        integration.setEnableRulePromotion(memoryIntegrationProperties.isEnableRulePromotion());
        integration.setEnableEpisodicInjection(memoryIntegrationProperties.isEnableEpisodicInjection());
        integration.setMarkUsedEnabled(memoryIntegrationProperties.isMarkUsedEnabled());
        integration.setMaxRules(memoryIntegrationProperties.getMaxRules());
        integration.setMaxSemanticMemories(memoryIntegrationProperties.getMaxSemanticMemories());
        integration.setMaxEpisodes(memoryIntegrationProperties.getMaxEpisodes());
        integration.setMaxConversationMessages(memoryIntegrationProperties.getMaxConversationMessages());
        integration.setMaxContextCharacters(memoryIntegrationProperties.getMaxContextCharacters());
        payload.setIntegration(integration);

        MemoryConfigurationPayload.Scoring scoring = new MemoryConfigurationPayload.Scoring();
        scoring.setImportanceWeight(memoryScoringProperties.getImportanceWeight());
        scoring.setUsageWeight(memoryScoringProperties.getUsageWeight());
        scoring.setRecencyWeight(memoryScoringProperties.getRecencyWeight());
        payload.setScoring(scoring);

        MemoryConfigurationPayload.GlobalContext globalContext = new MemoryConfigurationPayload.GlobalContext();
        globalContext.setEnabled(globalContextProperties.isEnabled());
        globalContext.setLoadMode(globalContextProperties.getLoadMode() == null ? null : globalContextProperties.getLoadMode().name());
        globalContext.setCacheRefreshInterval(globalContextProperties.getCacheRefreshInterval());
        globalContext.setFiles(globalContextProperties.getFiles() == null ? List.of() : List.copyOf(globalContextProperties.getFiles()));
        payload.setGlobalContext(globalContext);
        return payload;
    }

    private ArtifactsConfigurationPayload mergeArtifacts(
            ArtifactsConfigurationPayload existing,
            ArtifactsConfigurationPayload seed,
            MergeStats stats
    ) {
        ArtifactsConfigurationPayload merged = existing == null ? new ArtifactsConfigurationPayload() : existing;
        merged.setEnabled(completeValue(merged.getEnabled(), seed.getEnabled(), stats));
        merged.setContentFolder(completeString(merged.getContentFolder(), seed.getContentFolder(), stats));
        if (merged.getRetention() == null) {
            merged.setRetention(new ArtifactsConfigurationPayload.Retention());
            stats.markCompleted();
        }
        if (seed.getRetention() != null) {
            merged.getRetention().setByType(normalizeDurationMapKeys(merged.getRetention().getByType()));
            merged.getRetention().setDefaultTtl(
                    completeValue(merged.getRetention().getDefaultTtl(), seed.getRetention().getDefaultTtl(), stats)
            );
            merged.getRetention().setByType(mergeDurationMap(
                    merged.getRetention().getByType(),
                    seed.getRetention().getByType(),
                    stats
            ));
        }
        return merged;
    }

    private RetentionConfigurationPayload mergeRetention(
            RetentionConfigurationPayload existing,
            RetentionConfigurationPayload seed,
            MergeStats stats
    ) {
        RetentionConfigurationPayload merged = existing == null ? new RetentionConfigurationPayload() : existing;
        merged.setDefaultTtl(completeValue(merged.getDefaultTtl(), seed.getDefaultTtl(), stats));

        Map<String, RetentionConfigurationPayload.FamilyPolicyPayload> mergedFamilies = normalizeFamilyMapKeys(merged.getFamilies());
        for (Map.Entry<String, RetentionConfigurationPayload.FamilyPolicyPayload> seedEntry : seed.getFamilies().entrySet()) {
            String key = normalizeKey(seedEntry.getKey());
            RetentionConfigurationPayload.FamilyPolicyPayload existingFamily = mergedFamilies.get(key);
            if (existingFamily == null) {
                mergedFamilies.put(key, seedEntry.getValue());
                stats.markCompleted();
                continue;
            }

            RetentionConfigurationPayload.FamilyPolicyPayload seedFamily = seedEntry.getValue();
            existingFamily.setTtl(completeValue(existingFamily.getTtl(), seedFamily.getTtl(), stats));
            existingFamily.setDisposition(completeValue(existingFamily.getDisposition(), seedFamily.getDisposition(), stats));

            Map<String, RetentionConfigurationPayload.DomainPolicyPayload> domains = normalizeDomainMapKeys(existingFamily.getDomains());
            for (Map.Entry<String, RetentionConfigurationPayload.DomainPolicyPayload> seedDomain : seedFamily.getDomains().entrySet()) {
                String domainKey = normalizeKey(seedDomain.getKey());
                RetentionConfigurationPayload.DomainPolicyPayload existingDomain = domains.get(domainKey);
                if (existingDomain == null) {
                    domains.put(domainKey, seedDomain.getValue());
                    stats.markCompleted();
                    continue;
                }
                RetentionConfigurationPayload.DomainPolicyPayload seedDomainPolicy = seedDomain.getValue();
                existingDomain.setTtl(completeValue(existingDomain.getTtl(), seedDomainPolicy.getTtl(), stats));
                existingDomain.setDisposition(completeValue(existingDomain.getDisposition(), seedDomainPolicy.getDisposition(), stats));
            }
            existingFamily.setDomains(domains);
            mergedFamilies.put(key, existingFamily);
        }
        merged.setFamilies(mergedFamilies);
        return merged;
    }

    private MemoryConfigurationPayload mergeMemory(
            MemoryConfigurationPayload existing,
            MemoryConfigurationPayload seed,
            MergeStats stats
    ) {
        MemoryConfigurationPayload merged = existing == null ? new MemoryConfigurationPayload() : existing;
        merged.setConversation(mergeConversation(merged.getConversation(), seed.getConversation(), stats));
        merged.setContext(mergeContext(merged.getContext(), seed.getContext(), stats));
        merged.setRetrieval(mergeRetrieval(merged.getRetrieval(), seed.getRetrieval(), stats));
        merged.setIntegration(mergeIntegration(merged.getIntegration(), seed.getIntegration(), stats));
        merged.setScoring(mergeScoring(merged.getScoring(), seed.getScoring(), stats));
        merged.setGlobalContext(mergeGlobalContext(merged.getGlobalContext(), seed.getGlobalContext(), stats));
        return merged;
    }

    private MemoryConfigurationPayload.Conversation mergeConversation(MemoryConfigurationPayload.Conversation existing,
                                                                      MemoryConfigurationPayload.Conversation seed,
                                                                      MergeStats stats) {
        MemoryConfigurationPayload.Conversation merged = existing == null ? new MemoryConfigurationPayload.Conversation() : existing;
        if (existing == null) {
            stats.markCompleted();
        }
        merged.setEnabled(completeValue(merged.getEnabled(), seed.getEnabled(), stats));
        merged.setMaxRecentMessages(completeValue(merged.getMaxRecentMessages(), seed.getMaxRecentMessages(), stats));
        merged.setMaxRecentCharacters(completeValue(merged.getMaxRecentCharacters(), seed.getMaxRecentCharacters(), stats));
        merged.setMinMessagesToKeep(completeValue(merged.getMinMessagesToKeep(), seed.getMinMessagesToKeep(), stats));
        merged.setExpireAfterMinutes(completeValue(merged.getExpireAfterMinutes(), seed.getExpireAfterMinutes(), stats));
        merged.setAutoSummarize(completeValue(merged.getAutoSummarize(), seed.getAutoSummarize(), stats));
        merged.setMaxSummaryCharacters(completeValue(merged.getMaxSummaryCharacters(), seed.getMaxSummaryCharacters(), stats));
        return merged;
    }

    private MemoryConfigurationPayload.Context mergeContext(MemoryConfigurationPayload.Context existing,
                                                            MemoryConfigurationPayload.Context seed,
                                                            MergeStats stats) {
        MemoryConfigurationPayload.Context merged = existing == null ? new MemoryConfigurationPayload.Context() : existing;
        if (existing == null) {
            stats.markCompleted();
        }
        merged.setMaxRules(completeValue(merged.getMaxRules(), seed.getMaxRules(), stats));
        merged.setMaxMemories(completeValue(merged.getMaxMemories(), seed.getMaxMemories(), stats));
        merged.setMaxCharacters(completeValue(merged.getMaxCharacters(), seed.getMaxCharacters(), stats));
        merged.setMaxEpisodes(completeValue(merged.getMaxEpisodes(), seed.getMaxEpisodes(), stats));
        return merged;
    }

    private MemoryConfigurationPayload.Retrieval mergeRetrieval(MemoryConfigurationPayload.Retrieval existing,
                                                                MemoryConfigurationPayload.Retrieval seed,
                                                                MergeStats stats) {
        MemoryConfigurationPayload.Retrieval merged = existing == null ? new MemoryConfigurationPayload.Retrieval() : existing;
        if (existing == null) {
            stats.markCompleted();
        }
        merged.setMaxRules(completeValue(merged.getMaxRules(), seed.getMaxRules(), stats));
        merged.setMaxSemanticMemories(completeValue(merged.getMaxSemanticMemories(), seed.getMaxSemanticMemories(), stats));
        merged.setMaxCandidatePoolSize(completeValue(merged.getMaxCandidatePoolSize(), seed.getMaxCandidatePoolSize(), stats));
        merged.setMaxEpisodes(completeValue(merged.getMaxEpisodes(), seed.getMaxEpisodes(), stats));
        merged.setMaxProjectEpisodeFetch(completeValue(merged.getMaxProjectEpisodeFetch(), seed.getMaxProjectEpisodeFetch(), stats));
        merged.setConversationSliceMaxCharacters(completeValue(merged.getConversationSliceMaxCharacters(), seed.getConversationSliceMaxCharacters(), stats));
        return merged;
    }

    private MemoryConfigurationPayload.Integration mergeIntegration(MemoryConfigurationPayload.Integration existing,
                                                                    MemoryConfigurationPayload.Integration seed,
                                                                    MergeStats stats) {
        MemoryConfigurationPayload.Integration merged = existing == null ? new MemoryConfigurationPayload.Integration() : existing;
        if (existing == null) {
            stats.markCompleted();
        }
        merged.setEnableSemanticExtraction(completeValue(merged.getEnableSemanticExtraction(), seed.getEnableSemanticExtraction(), stats));
        merged.setEnableRulePromotion(completeValue(merged.getEnableRulePromotion(), seed.getEnableRulePromotion(), stats));
        merged.setEnableEpisodicInjection(completeValue(merged.getEnableEpisodicInjection(), seed.getEnableEpisodicInjection(), stats));
        merged.setMarkUsedEnabled(completeValue(merged.getMarkUsedEnabled(), seed.getMarkUsedEnabled(), stats));
        merged.setMaxRules(completeValue(merged.getMaxRules(), seed.getMaxRules(), stats));
        merged.setMaxSemanticMemories(completeValue(merged.getMaxSemanticMemories(), seed.getMaxSemanticMemories(), stats));
        merged.setMaxEpisodes(completeValue(merged.getMaxEpisodes(), seed.getMaxEpisodes(), stats));
        merged.setMaxConversationMessages(completeValue(merged.getMaxConversationMessages(), seed.getMaxConversationMessages(), stats));
        merged.setMaxContextCharacters(completeValue(merged.getMaxContextCharacters(), seed.getMaxContextCharacters(), stats));
        return merged;
    }

    private MemoryConfigurationPayload.Scoring mergeScoring(MemoryConfigurationPayload.Scoring existing,
                                                            MemoryConfigurationPayload.Scoring seed,
                                                            MergeStats stats) {
        MemoryConfigurationPayload.Scoring merged = existing == null ? new MemoryConfigurationPayload.Scoring() : existing;
        if (existing == null) {
            stats.markCompleted();
        }
        merged.setImportanceWeight(completeValue(merged.getImportanceWeight(), seed.getImportanceWeight(), stats));
        merged.setUsageWeight(completeValue(merged.getUsageWeight(), seed.getUsageWeight(), stats));
        merged.setRecencyWeight(completeValue(merged.getRecencyWeight(), seed.getRecencyWeight(), stats));
        return merged;
    }

    private MemoryConfigurationPayload.GlobalContext mergeGlobalContext(MemoryConfigurationPayload.GlobalContext existing,
                                                                        MemoryConfigurationPayload.GlobalContext seed,
                                                                        MergeStats stats) {
        MemoryConfigurationPayload.GlobalContext merged = existing == null ? new MemoryConfigurationPayload.GlobalContext() : existing;
        if (existing == null) {
            stats.markCompleted();
        }
        merged.setEnabled(completeValue(merged.getEnabled(), seed.getEnabled(), stats));
        merged.setLoadMode(completeString(merged.getLoadMode(), seed.getLoadMode(), stats));
        merged.setCacheRefreshInterval(completeValue(merged.getCacheRefreshInterval(), seed.getCacheRefreshInterval(), stats));
        if (merged.getFiles() == null || merged.getFiles().isEmpty()) {
            if (seed.getFiles() != null && !seed.getFiles().isEmpty()) {
                merged.setFiles(List.copyOf(seed.getFiles()));
                stats.markCompleted();
            }
        }
        return merged;
    }

    private Map<String, Duration> mergeDurationMap(Map<String, Duration> existing, Map<String, Duration> seed, MergeStats stats) {
        Map<String, Duration> merged = existing == null ? new LinkedHashMap<>() : new LinkedHashMap<>(existing);
        if (seed == null) {
            return merged;
        }
        for (Map.Entry<String, Duration> entry : seed.entrySet()) {
            String key = normalizeKey(entry.getKey());
            if (!merged.containsKey(key) || merged.get(key) == null) {
                merged.put(key, entry.getValue());
                stats.markCompleted();
            }
        }
        return merged;
    }

    private Map<String, Duration> normalizeDurationMapKeys(Map<String, Duration> source) {
        Map<String, Duration> normalized = new LinkedHashMap<>();
        if (source == null) {
            return normalized;
        }
        for (Map.Entry<String, Duration> entry : source.entrySet()) {
            String key = normalizeKey(entry.getKey());
            if (!normalized.containsKey(key)) {
                normalized.put(key, entry.getValue());
                continue;
            }
            if (normalized.get(key) == null && entry.getValue() != null) {
                normalized.put(key, entry.getValue());
            }
        }
        return normalized;
    }

    private Map<String, RetentionConfigurationPayload.FamilyPolicyPayload> normalizeFamilyMapKeys(
            Map<String, RetentionConfigurationPayload.FamilyPolicyPayload> source
    ) {
        Map<String, RetentionConfigurationPayload.FamilyPolicyPayload> normalized = new LinkedHashMap<>();
        if (source == null) {
            return normalized;
        }
        for (Map.Entry<String, RetentionConfigurationPayload.FamilyPolicyPayload> entry : source.entrySet()) {
            String key = normalizeKey(entry.getKey());
            if (!normalized.containsKey(key)) {
                normalized.put(key, entry.getValue());
            }
        }
        return normalized;
    }

    private Map<String, RetentionConfigurationPayload.DomainPolicyPayload> normalizeDomainMapKeys(
            Map<String, RetentionConfigurationPayload.DomainPolicyPayload> source
    ) {
        Map<String, RetentionConfigurationPayload.DomainPolicyPayload> normalized = new LinkedHashMap<>();
        if (source == null) {
            return normalized;
        }
        for (Map.Entry<String, RetentionConfigurationPayload.DomainPolicyPayload> entry : source.entrySet()) {
            String key = normalizeKey(entry.getKey());
            if (!normalized.containsKey(key)) {
                normalized.put(key, entry.getValue());
            }
        }
        return normalized;
    }

    private <T> T completeValue(T existingValue, T seedValue, MergeStats stats) {
        if (existingValue != null || seedValue == null) {
            return existingValue;
        }
        stats.markCompleted();
        return seedValue;
    }

    private String completeString(String existingValue, String seedValue, MergeStats stats) {
        if (!isBlank(existingValue) || isBlank(seedValue)) {
            return existingValue;
        }
        stats.markCompleted();
        return seedValue.trim();
    }

    private String coalesceNonBlank(String preferred, String fallback) {
        if (!isBlank(preferred)) {
            return preferred.trim();
        }
        return isBlank(fallback) ? preferred : fallback.trim();
    }

    private boolean wasCompleted(String before, String after) {
        return isBlank(before) && !isBlank(after);
    }

    private String safeProviderName(OpenAiLikeProperties provider) {
        if (provider == null || isBlank(provider.name())) {
            return "<unknown>";
        }
        return provider.name().trim();
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT).replace('-', '_').replace(' ', '_').replace('.', '_');
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static final class MergeStats {
        private int completedFields = 0;

        void markCompleted() {
            completedFields++;
        }

        int completedFields() {
            return completedFields;
        }

        boolean changed() {
            return completedFields > 0;
        }
    }
}
