package org.qubership.cloud.configserver.config.pojo;

import java.util.Optional;

public class EncryptionConfig {

    private Optional<String> keyPassword;
    private String key;

    public Optional<String> getKeyPassword() {
        return keyPassword;
    }
    public void setKeyPassword(Optional<String> keyPassword) {
        this.keyPassword = keyPassword;
    }
    public String getKey() {
        return key;
    }
    public void setKey(String key) {
        this.key = key;
    }

}
