package fr.ses10doigts.toolkitbridge.repository;

import fr.ses10doigts.toolkitbridge.model.entity.BotAccount;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BotAccountRepository extends JpaRepository<BotAccount, UUID> {

    Optional<BotAccount> findByBotIdent(String botIdent);

    Optional<BotAccount> findByApiKeyPrefixAndEnabledTrue(String apiKeyPrefix);

    boolean existsByBotIdent(String botIdent);
}