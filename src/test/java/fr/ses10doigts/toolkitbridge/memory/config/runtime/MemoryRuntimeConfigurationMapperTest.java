package fr.ses10doigts.toolkitbridge.memory.config.runtime;

import fr.ses10doigts.toolkitbridge.service.configuration.admin.payload.MemoryConfigurationPayload;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MemoryRuntimeConfigurationMapperTest {

    @Test
    void shouldIgnoreIntegrationSizingFieldsNotAppliedByRuntime() {
        MemoryRuntimeConfigurationMapper mapper = new MemoryRuntimeConfigurationMapper();

        MemoryConfigurationPayload payloadA = payloadWithIntegrationSizing(1, 2, 3, 4, 5);
        MemoryConfigurationPayload payloadB = payloadWithIntegrationSizing(10, 20, 30, 40, 50);

        MemoryRuntimeConfiguration runtimeA = mapper.toRuntimeConfiguration(payloadA);
        MemoryRuntimeConfiguration runtimeB = mapper.toRuntimeConfiguration(payloadB);

        assertThat(runtimeA).isEqualTo(runtimeB);
    }

    private MemoryConfigurationPayload payloadWithIntegrationSizing(
            int maxRules,
            int maxSemanticMemories,
            int maxEpisodes,
            int maxConversationMessages,
            int maxContextCharacters
    ) {
        MemoryConfigurationPayload payload = new MemoryConfigurationPayload();
        payload.getIntegration().setEnableSemanticExtraction(Boolean.TRUE);
        payload.getIntegration().setEnableRulePromotion(Boolean.TRUE);
        payload.getIntegration().setEnableEpisodicInjection(Boolean.TRUE);
        payload.getIntegration().setMarkUsedEnabled(Boolean.TRUE);
        payload.getIntegration().setMaxRules(maxRules);
        payload.getIntegration().setMaxSemanticMemories(maxSemanticMemories);
        payload.getIntegration().setMaxEpisodes(maxEpisodes);
        payload.getIntegration().setMaxConversationMessages(maxConversationMessages);
        payload.getIntegration().setMaxContextCharacters(maxContextCharacters);
        return payload;
    }
}
