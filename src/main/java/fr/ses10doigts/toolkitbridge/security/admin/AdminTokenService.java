package fr.ses10doigts.toolkitbridge.security.admin;

import fr.ses10doigts.toolkitbridge.security.admin.config.AdminSecurityProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminTokenService {

    private static final int TOKEN_LENGTH_BYTES = 32;
    private static final Pattern TOKEN_FORMAT = Pattern.compile("^[A-Za-z0-9_-]{32,128}$");

    private final AdminSecurityProperties securityProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    private volatile String masterToken;

    public synchronized void initializeTokenIfNeeded() {
        if (masterToken != null) {
            return;
        }

        Path tokenPath = securityProperties.resolveMasterTokenPath();
        try {
            ensureParentDirectoryExists(tokenPath);

            if (Files.exists(tokenPath)) {
                String existingToken = readToken(tokenPath);
                if (existingToken != null) {
                    masterToken = existingToken;
                    return;
                }

                log.warn("Admin master token file '{}' is empty or invalid. Regenerating a new token.", tokenPath);
            }

            masterToken = generateAndPersistToken(tokenPath);
            log.info("Marcel admin master token generated. Store it now; it will not be shown again: {}", masterToken);
        } catch (IOException ex) {
            throw new IllegalStateException("Unable to load or generate admin master token", ex);
        }
    }

    public String getMasterToken() {
        initializeTokenIfNeeded();
        return masterToken;
    }

    public boolean matches(String candidateToken) {
        if (candidateToken == null) {
            return false;
        }

        String sanitizedCandidate = candidateToken.trim();
        if (sanitizedCandidate.isEmpty()) {
            return false;
        }

        byte[] expected = getMasterToken().getBytes(StandardCharsets.UTF_8);
        byte[] provided = sanitizedCandidate.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, provided);
    }

    public Path getMasterTokenPath() {
        return securityProperties.resolveMasterTokenPath();
    }

    private void ensureParentDirectoryExists(Path tokenPath) throws IOException {
        Path parent = tokenPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private String readToken(Path tokenPath) throws IOException {
        String content = Files.readString(tokenPath, StandardCharsets.UTF_8);
        String[] lines = content.split("\\R");
        for (String line : lines) {
            String candidate = line.trim();
            if (!candidate.isEmpty() && isTokenFormatValid(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private String generateAndPersistToken(Path tokenPath) throws IOException {
        byte[] randomBytes = new byte[TOKEN_LENGTH_BYTES];
        secureRandom.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        Path tempPath = tokenPath.resolveSibling(tokenPath.getFileName() + ".tmp");
        Files.writeString(
                tempPath,
                token + System.lineSeparator(),
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE
        );

        try {
            Files.move(tempPath, tokenPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException ex) {
            Files.move(tempPath, tokenPath, StandardCopyOption.REPLACE_EXISTING);
        }

        return token;
    }

    private boolean isTokenFormatValid(String token) {
        return TOKEN_FORMAT.matcher(token).matches();
    }
}
