package fr.ses10doigts.toolkitbridge.security.admin.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Path;

@ConfigurationProperties(prefix = "toolkit.admin.security")
public class AdminSecurityProperties {

    private String masterTokenFile = "data/runtime/security/admin-master.token";

    public String getMasterTokenFile() {
        return masterTokenFile;
    }

    public void setMasterTokenFile(String masterTokenFile) {
        this.masterTokenFile = masterTokenFile;
    }

    public Path resolveMasterTokenPath() {
        return Path.of(masterTokenFile).toAbsolutePath().normalize();
    }
}
