package fr.ses10doigts.toolkitbridge.service.admin;

import fr.ses10doigts.toolkitbridge.model.dto.admin.technical.TechnicalAdminView;
import fr.ses10doigts.toolkitbridge.persistence.model.PersistableObjectFamily;
import fr.ses10doigts.toolkitbridge.persistence.retention.PersistenceRetentionPolicyResolver;
import fr.ses10doigts.toolkitbridge.persistence.retention.RetentionPolicy;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RetentionQueryService {

    private final PersistenceRetentionPolicyResolver retentionPolicyResolver;

    public RetentionQueryService(PersistenceRetentionPolicyResolver retentionPolicyResolver) {
        this.retentionPolicyResolver = retentionPolicyResolver;
    }

    public List<TechnicalAdminView.RetentionItem> listRetentionPolicies() {
        return java.util.Arrays.stream(PersistableObjectFamily.values())
                .map(retentionPolicyResolver::resolve)
                .map(this::toRetentionItem)
                .toList();
    }

    private TechnicalAdminView.RetentionItem toRetentionItem(RetentionPolicy policy) {
        return new TechnicalAdminView.RetentionItem(
                policy.family().name(),
                policy.ttl(),
                policy.disposition().name()
        );
    }
}
