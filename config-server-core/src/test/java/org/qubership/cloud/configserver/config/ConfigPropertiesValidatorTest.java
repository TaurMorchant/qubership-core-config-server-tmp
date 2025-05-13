package org.qubership.cloud.configserver.config;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class ConfigPropertiesValidatorTest {

    private ConfigPropertiesValidator validator = new ConfigPropertiesValidator();

    @Test
    public void validateKeysWithoutConflicts() {
        Map<String, ConfigProperty> source = new HashMap<>();
        source.put("a.b.c.d", null);
        source.put("a.b.c.e", null);
        source.put("a.c.b", null);
        source.put("a.c.bb.d", null);
        source.put("a.c.d", null);
        assertTrue(validator.validate(source).isValid());
    }

    @Test
    public void validateKeysWithConflicts() {
        Map<String, ConfigProperty> source = new HashMap<>();
        final String conflictingKey11 = "a.b";
        final String conflictingKey12 = "a.b.c";
        final String conflictingKey21 = "f.g";
        final String conflictingKey22 = "f.g.h";
        source.put(conflictingKey11, null);
        source.put(conflictingKey12, null);
        source.put(conflictingKey21, null);
        source.put(conflictingKey22, null);

        ConfigPropertiesValidator.ValidationResult result = validator.validate(source);

        assertFalse(result.isValid());
        assertEquals(result.getConflictingProperties().size(), 2);
        assertTrue(result.getConflictingProperties().contains(conflictingKey11 + " conflicts with " + conflictingKey12) || result.getConflictingProperties().contains(conflictingKey12 + " conflicts with " + conflictingKey11));
        assertTrue(result.getConflictingProperties().contains(conflictingKey21 + " conflicts with " + conflictingKey22) || result.getConflictingProperties().contains(conflictingKey22 + " conflicts with " + conflictingKey21));
    }

    @Test
    public void validateKeyStartWithDot() {
        Map<String, ConfigProperty> source = new HashMap<>();
        final String conflictingKey1 = ".a.b";
        final String conflictingKey2 = ".a.b.c";
        source.put(conflictingKey1, null);
        source.put(conflictingKey2, null);

        ConfigPropertiesValidator.ValidationResult result = validator.validate(source);
        assertFalse(result.isValid());
        assertTrue(result.getDotProperties().contains(conflictingKey1 + " start with dot")
               && result.getDotProperties().contains(conflictingKey2 + " start with dot"));
    }

    @Test
    public void validateKeysWithConflictsAndStartWithDot() {
        Map<String, ConfigProperty> source = new HashMap<>();
        final String conflictingKey11 = "a.b";
        final String conflictingKey12 = "a.b.c";
        final String conflictingKey21 = "f.g";
        final String conflictingKey22 = "f.g.h";
        final String conflictingKey3 = ".i.j";
        final String conflictingKey4 = ".k.l.m";
        source.put(conflictingKey11, null);
        source.put(conflictingKey12, null);
        source.put(conflictingKey21, null);
        source.put(conflictingKey22, null);
        source.put(conflictingKey3, null);
        source.put(conflictingKey4, null);

        ConfigPropertiesValidator.ValidationResult result = validator.validate(source);

        assertFalse(result.isValid());
        assertTrue(result.getConflictingProperties().contains(conflictingKey11 + " conflicts with " + conflictingKey12) || result.getConflictingProperties().contains(conflictingKey12 + " conflicts with " + conflictingKey11));
        assertTrue(result.getConflictingProperties().contains(conflictingKey21 + " conflicts with " + conflictingKey22) || result.getConflictingProperties().contains(conflictingKey22 + " conflicts with " + conflictingKey21));
        assertTrue(result.getDotProperties().contains(conflictingKey3 + " start with dot")
                && result.getDotProperties().contains(conflictingKey4 + " start with dot"));
    }
}