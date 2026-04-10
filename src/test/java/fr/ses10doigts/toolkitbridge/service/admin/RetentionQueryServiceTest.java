package fr.ses10doigts.toolkitbridge.service.admin;

import fr.ses10doigts.toolkitbridge.model.dto.admin.technical.TechnicalAdminView;
import fr.ses10doigts.toolkitbridge.persistence.model.PersistableObjectFamily;
import fr.ses10doigts.toolkitbridge.persistence.retention.PersistenceRetentionPolicyResolver;
import fr.ses10doigts.toolkitbridge.persistence.retention.RetentionDisposition;
import fr.ses10doigts.toolkitbridge.persistence.retention.RetentionPolicy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RetentionQueryServiceTest {

    @Test
    void listRetentionPoliciesIteratesFamiliesAndMapsRetentionItems() {
        PersistenceRetentionPolicyResolver resolver = mock(PersistenceRetentionPolicyResolver.class);
        when(resolver.resolve(any(PersistableObjectFamily.class)))
                .thenAnswer(invocation -> {
                    PersistableObjectFamily family = invocation.getArgument(0);
                    int index = family.ordinal() + 1;
                    RetentionDisposition disposition = index % 2 == 0
                            ? RetentionDisposition.PRESERVE
                            : RetentionDisposition.PURGE;
                    return new RetentionPolicy(family, null, Duration.ofDays(index), disposition);
                });

        RetentionQueryService service = new RetentionQueryService(resolver);

        List<TechnicalAdminView.RetentionItem> items = service.listRetentionPolicies();
        List<String> expectedFamilies = java.util.Arrays.stream(PersistableObjectFamily.values())
                .map(Enum::name)
                .toList();
        List<Duration> expectedTtls = new ArrayList<>();
        List<String> expectedDispositions = new ArrayList<>();
        for (PersistableObjectFamily family : PersistableObjectFamily.values()) {
            int index = family.ordinal() + 1;
            expectedTtls.add(Duration.ofDays(index));
            expectedDispositions.add(index % 2 == 0 ? "PRESERVE" : "PURGE");
        }

        assertThat(items).hasSize(PersistableObjectFamily.values().length);
        assertThat(items).extracting(TechnicalAdminView.RetentionItem::family)
                .containsExactlyElementsOf(expectedFamilies);
        assertThat(items).extracting(TechnicalAdminView.RetentionItem::ttl)
                .containsExactlyElementsOf(expectedTtls);
        assertThat(items).extracting(TechnicalAdminView.RetentionItem::disposition)
                .containsExactlyElementsOf(expectedDispositions);

        for (PersistableObjectFamily family : PersistableObjectFamily.values()) {
            verify(resolver).resolve(family);
        }
    }
}
