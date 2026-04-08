package fr.ses10doigts.toolkitbridge.security.admin;

import fr.ses10doigts.toolkitbridge.security.admin.config.AdminSecurityProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class AdminTokenServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void generatesAndPersistsTokenOnFirstInitialization() {
        Path tokenPath = tempDir.resolve("security").resolve("admin-master.token");
        AdminTokenService service = new AdminTokenService(buildProperties(tokenPath));

        service.initializeTokenIfNeeded();

        String token = service.getMasterToken();
        assertThat(token).isNotBlank();
        assertThat(Files.exists(tokenPath)).isTrue();
        assertThat(readToken(tokenPath)).isEqualTo(token);
    }

    @Test
    void loadsExistingTokenWithoutRegeneration() throws IOException {
        Path tokenPath = tempDir.resolve("security").resolve("admin-master.token");
        Files.createDirectories(tokenPath.getParent());
        String existingToken = "existingtokenvalueexistingtokenvalue1234567";
        Files.writeString(tokenPath, existingToken + "\n", StandardCharsets.UTF_8);

        AdminTokenService service = new AdminTokenService(buildProperties(tokenPath));

        service.initializeTokenIfNeeded();

        assertThat(service.getMasterToken()).isEqualTo(existingToken);
        assertThat(readToken(tokenPath)).isEqualTo(existingToken);
    }

    @Test
    void regeneratesTokenWhenStoredFileIsBlank() throws IOException {
        Path tokenPath = tempDir.resolve("security").resolve("admin-master.token");
        Files.createDirectories(tokenPath.getParent());
        Files.writeString(tokenPath, "   \n", StandardCharsets.UTF_8);

        AdminTokenService service = new AdminTokenService(buildProperties(tokenPath));

        service.initializeTokenIfNeeded();

        String generated = service.getMasterToken();
        assertThat(generated).isNotBlank();
        assertThat(readToken(tokenPath)).isEqualTo(generated);
    }

    private AdminSecurityProperties buildProperties(Path tokenPath) {
        AdminSecurityProperties properties = new AdminSecurityProperties();
        properties.setMasterTokenFile(tokenPath.toString());
        return properties;
    }

    private String readToken(Path tokenPath) {
        try {
            return Files.readString(tokenPath, StandardCharsets.UTF_8).trim();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }
}
