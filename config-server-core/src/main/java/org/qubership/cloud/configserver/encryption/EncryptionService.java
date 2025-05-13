package org.qubership.cloud.configserver.encryption;

import org.qubership.cloud.configserver.config.ConfigProperty;

import java.util.Map;

/**
 * Provides utility methods for properties encryption/decryption
 */
public interface EncryptionService {
    /**
     * Ensures incoming values for properties that are already encrypted will be also encrypted
     * @param currentProperties properties already present on CS
     * @param properties incoming properties
     */
    public void encryptUpdatedPropertiesIfNeeded(Map<String, ConfigProperty> currentProperties, Map<String, ConfigProperty> properties);

    /**
     * Returns a map of properties from config profile, decrypting encrypting values
     * @param encryptedMap map of properties some of which may be encrypted
     * @return decrypted map of properties in key:value format
     */
    public Map<String, String> getDecryptedPropertiesAsMap(Map<String, ConfigProperty> encryptedMap);

    /**
     * Checks property encryption status and decrypts value if necessary
     * @param potentiallyEncryptedProperty
     * @return decrypted property value
     */
    public String decryptPropertyIfRequired(ConfigProperty potentiallyEncryptedProperty);
}
