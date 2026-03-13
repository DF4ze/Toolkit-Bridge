package fr.ses10doigts.toolkitbridge.service.auth;

import fr.ses10doigts.toolkitbridge.exception.BotAlreadyExistsException;
import fr.ses10doigts.toolkitbridge.exception.InvalidApiKeyException;
import fr.ses10doigts.toolkitbridge.model.dto.auth.AuthenticatedBot;
import fr.ses10doigts.toolkitbridge.model.dto.auth.BotProvisioningResult;
import fr.ses10doigts.toolkitbridge.model.entity.BotAccount;
import fr.ses10doigts.toolkitbridge.repository.BotAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class BotAccountService {

    private static final String API_KEY_PREFIX = "tb_";
    private static final int LOOKUP_PREFIX_LENGTH = 8;
    private static final int SECRET_LENGTH = 32;

    private final BotAccountRepository botAccountRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional
    public BotProvisioningResult createBot(String botIdent) {
        validateBotIdent(botIdent);

        if (botAccountRepository.existsByBotIdent(botIdent)) {
            throw new BotAlreadyExistsException(botIdent);
        }

        String lookupPrefix = generateRandomToken(LOOKUP_PREFIX_LENGTH);
        String secretPart = generateRandomToken(SECRET_LENGTH);
        String apiKey = API_KEY_PREFIX + lookupPrefix + "." + secretPart;

        BotAccount botAccount = new BotAccount();
        botAccount.setBotIdent(botIdent);
        botAccount.setApiKeyPrefix(lookupPrefix);
        botAccount.setApiKeyHash(hashApiKey(apiKey));
        botAccount.setEnabled(true);

        botAccountRepository.save(botAccount);

        return new BotProvisioningResult(botIdent, apiKey);
    }

    @Transactional(readOnly = true)
    public AuthenticatedBot authenticate(String rawApiKey) {
        if (rawApiKey == null || rawApiKey.isBlank()) {
            throw new InvalidApiKeyException();
        }

        String lookupPrefix = extractLookupPrefix(rawApiKey);

        BotAccount botAccount = findEnabledByApiKeyPrefix(lookupPrefix);

        if (!matchesApiKey(botAccount, rawApiKey)) {
            throw new InvalidApiKeyException();
        }

        return new AuthenticatedBot(
                botAccount.getId(),
                botAccount.getBotIdent()
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

    public BotAccount findEnabledByApiKeyPrefix(String apiKeyPrefix) {
        return botAccountRepository.findByApiKeyPrefixAndEnabledTrue(apiKeyPrefix)
                .orElseThrow(InvalidApiKeyException::new);
    }

    public boolean matchesApiKey(BotAccount botAccount, String rawApiKey) {
        return hashApiKey(rawApiKey).equals(botAccount.getApiKeyHash());
    }

    private void validateBotIdent(String botIdent) {
        if (botIdent == null || botIdent.isBlank()) {
            throw new IllegalArgumentException("botIdent cannot be empty");
        }

        if (botIdent.length() > 100) {
            throw new IllegalArgumentException("botIdent is too long");
        }

        if (!botIdent.matches("[a-zA-Z0-9._-]+")) {
            throw new IllegalArgumentException("botIdent contains forbidden characters");
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