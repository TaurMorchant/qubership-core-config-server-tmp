package org.qubership.cloud.configserver.encryption.service;

import org.qubership.cloud.configserver.config.ConfigProperty;
import org.qubership.cloud.configserver.encryption.EncryptionService;

import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class EncryptionServiceImpl implements EncryptionService {

    public void encryptUpdatedPropertiesIfNeeded(Map<String, ConfigProperty> currentProperties, Map<String, ConfigProperty> properties) {
        // not supported
    }

    public Map<String, String> getDecryptedPropertiesAsMap(Map<String, ConfigProperty> encryptedMap) {
        Map<String, String> result = new HashMap<>(encryptedMap.size());
        for (ConfigProperty property : encryptedMap.values()) {
            String value = property.getValue();
            if (property.isEncrypted()) {
                log.warn("Encrypted properties is not supported");
            }
            result.put(property.getKey(), value);
        }
        return result;
    }

    public String decryptPropertyIfRequired(ConfigProperty potentiallyEncryptedProperty) {
        if (potentiallyEncryptedProperty.isEncrypted()) {
            log.warn("Encrypted properties is not supported");
        }
        return potentiallyEncryptedProperty.getValue();
    }
}
