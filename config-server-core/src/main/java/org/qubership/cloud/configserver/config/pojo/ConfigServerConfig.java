package org.qubership.cloud.configserver.config.pojo;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "config-server")
public class ConfigServerConfig {

    private EncryptionConfig encryption;

    public EncryptionConfig getEncryption() {
        return encryption;
    }

    public void setEncryption(EncryptionConfig encryptionConfig) {
        this.encryption = encryptionConfig;
    }

}
