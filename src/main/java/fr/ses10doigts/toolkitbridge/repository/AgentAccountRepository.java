package fr.ses10doigts.toolkitbridge.repository;

import fr.ses10doigts.toolkitbridge.model.entity.AgentAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AgentAccountRepository extends JpaRepository<AgentAccount, UUID> {

    Optional<AgentAccount> findByAgentIdent(String botIdent);

    Optional<AgentAccount> findByApiKeyPrefixAndEnabledTrue(String apiKeyPrefix);

    boolean existsByAgentIdent(String AgentIdent);
}