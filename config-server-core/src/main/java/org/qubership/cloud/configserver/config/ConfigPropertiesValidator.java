package org.qubership.cloud.configserver.config;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * This class is used for config properties validation,
 * which is performed before saving properties to database.
 * Validation result is returned as {@link ValidationResult}
 * instance and can be used for detailed error message.
 */
public class ConfigPropertiesValidator {

    private static final String CONFLICT_WITH = "%s conflicts with %s";
    private static final String START_WITH_DOT = "%s start with dot";

    public ValidationResult validate(Map<String, ConfigProperty> source) {
        ValidationResult validationResult = new ValidationResult();
        validateDotProperty(source, validationResult);
        validateConflictProperties(source, validationResult);

        if (validationResult.getConflictingProperties().isEmpty() && validationResult.getDotProperties().isEmpty()) {
            validationResult.valid();
        }
        return validationResult;
    }

    private void validateConflictProperties(Map<String, ConfigProperty> source, ValidationResult validationResult) {
        for (String key : source.keySet()) {
            for (String anotherKey : source.keySet()) {
                if (keysConflict(key, anotherKey)) {
                    if (!validationResult.getConflictingProperties().contains(String.format(CONFLICT_WITH, anotherKey, key))) {
                        validationResult.getConflictingProperties().add(String.format(CONFLICT_WITH, key, anotherKey));
                    }
                }
            }
        }
    }

    private boolean keysConflict(String key1, String key2) {
        if (key1.length() == key2.length()) return false;
        String longerKey, shorterKey;
        if (key1.length() > key2.length()) {
            longerKey = key1;
            shorterKey = key2;
        } else {
            longerKey = key2;
            shorterKey = key1;
        }
        return longerKey.startsWith(shorterKey) && longerKey.charAt(shorterKey.length()) == '.';
    }

    private void validateDotProperty(Map<String, ConfigProperty> mapOfConfigProperties, ValidationResult validationResult) {

        mapOfConfigProperties.keySet()
                .stream()
                .filter(key -> key.startsWith("."))
                .forEach(key -> validationResult.getDotProperties().add(String.format(START_WITH_DOT, key)));
    }

    /**
     * Represents config properties validation results.
     * Instance of this class contains conflicting properties keys,
     * which can be used in error message details.
     */
    @NoArgsConstructor
    public static class ValidationResult {
        @Getter
        private boolean valid = false;
        @Getter
        private Set<String> conflictingProperties = new HashSet<>();
        @Getter
        private Set<String> dotProperties = new HashSet<>();

        public void valid() {
            valid = true;
        }
    }
}
