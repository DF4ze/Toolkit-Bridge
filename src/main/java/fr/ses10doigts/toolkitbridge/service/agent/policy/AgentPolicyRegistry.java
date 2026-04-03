package fr.ses10doigts.toolkitbridge.service.agent.policy;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class AgentPolicyRegistry {

    private final List<AgentPolicy> policies;

    public AgentPolicyRegistry(List<AgentPolicy> policies) {
        this.policies = policies;
    }

    public AgentPolicy getRequired(String policyName) {
        String expected = normalize(policyName);
        return policies.stream()
                .filter(policy -> normalize(policy.name()).equals(expected))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown policy: " + policyName));
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
