package fr.ses10doigts.toolkitbridge.service.auth;

import fr.ses10doigts.toolkitbridge.exception.AgentAlreadyExistsException;
import fr.ses10doigts.toolkitbridge.exception.InvalidApiKeyException;
import fr.ses10doigts.toolkitbridge.model.dto.auth.AuthenticatedAgent;
import fr.ses10doigts.toolkitbridge.model.dto.auth.AgentProvisioningResult;
import fr.ses10doigts.toolkitbridge.model.entity.AgentAccount;
import fr.ses10doigts.toolkitbridge.repository.AgentAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class AgentAccountService {

    private static final String API_KEY_PREFIX = "tb_";
    private static final int LOOKUP_PREFIX_LENGTH = 8;
    private static final int SECRET_LENGTH = 32;

    private final AgentAccountRepository agentAccountRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public AgentProvisioningResult createAgent(String agentId) {
        validateAgentIdent(agentId);

        if (agentAccountRepository.existsByAgentIdent(agentId)) {
            throw new AgentAlreadyExistsException(agentId);
        }

        String lookupPrefix = generateRandomToken(LOOKUP_PREFIX_LENGTH);
        String secretPart = generateRandomToken(SECRET_LENGTH);
        String apiKey = API_KEY_PREFIX + lookupPrefix + "." + secretPart;

        AgentAccount agentAccount = new AgentAccount();
        agentAccount.setAgentIdent(agentId);
        agentAccount.setApiKeyPrefix(lookupPrefix);
        agentAccount.setApiKeyHash(hashApiKey(apiKey));
        agentAccount.setEnabled(true);

        agentAccountRepository.save(agentAccount);

        return new AgentProvisioningResult(agentId, apiKey);
    }

    @Transactional(readOnly = true)
    public AuthenticatedAgent authenticate(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.isBlank()) {
            throw new InvalidApiKeyException();
        }

        String lookupPrefix = extractLookupPrefix(rawApiKey);

        AgentAccount agentAccount = findEnabledByApiKeyPrefix(lookupPrefix);

        if (!matchesApiKey(agentAccount, rawApiKey)) {
            throw new InvalidApiKeyException();
        }

        return new AuthenticatedAgent(
                agentAccount.getId(),
                agentAccount.getAgentIdent()
        );
    }

    @Transactional(readOnly = true)
    public AuthenticatedAgent authenticateByAgentIdent(String agentIdent) {
        if (agentIdent == null || agentIdent.isBlank()) {
            throw new InvalidApiKeyException();
        }

        AgentAccount agentAccount = agentAccountRepository.findByAgentIdent(agentIdent)
                .orElseThrow(InvalidApiKeyException::new);

        if (!agentAccount.isEnabled()) {
            throw new InvalidApiKeyException();
        }

        return new AuthenticatedAgent(
                agentAccount.getId(),
                agentAccount.getAgentIdent()
        );
    }

    private String extractLookupPrefix(String rawApiKey) {
        if (!rawApiKey.startsWith(API_KEY_PREFIX)) {
            throw new InvalidApiKeyException();
        }

        int dotIndex = rawApiKey.indexOf('.');
        if (dotIndex <= API_KEY_PREFIX.length()) {
            throw new InvalidApiKeyException();
        }

        String lookupPrefix = rawApiKey.substring(API_KEY_PREFIX.length(), dotIndex);

        if (lookupPrefix.length() != LOOKUP_PREFIX_LENGTH) {
            throw new InvalidApiKeyException();
        }

        return lookupPrefix;
    }

    public AgentAccount findEnabledByApiKeyPrefix(String apiKeyPrefix) {
        return agentAccountRepository.findByApiKeyPrefixAndEnabledTrue(apiKeyPrefix)
                .orElseThrow(InvalidApiKeyException::new);
    }

    public boolean matchesApiKey(AgentAccount agentAccount, String rawApiKey) {
        return hashApiKey(rawApiKey).equals(agentAccount.getApiKeyHash());
    }

    private void validateAgentIdent(String agentIdent) {
        if (agentIdent == null || agentIdent.isBlank()) {
            throw new IllegalArgumentException("agentId cannot be empty");
        }

        if (agentIdent.length() > 100) {
            throw new IllegalArgumentException("agentId is too long");
        }

        if (!agentIdent.matches("[a-zA-Z0-9._-]+")) {
            throw new IllegalArgumentException("agentId contains forbidden characters");
        }
    }

    private String generateRandomToken(int length) {
        final String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int index = secureRandom.nextInt(alphabet.length());
            sb.append(alphabet.charAt(index));
        }

        return sb.toString();
    }

    private String hashApiKey(String rawApiKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawApiKey.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash API key", e);
        }
    }

}
