package org.qubership.cloud.configserver.config.service;

import org.qubership.cloud.configserver.config.ConfigProfile;
import org.qubership.cloud.configserver.config.ConfigProperty;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.lang3.RandomStringUtils;

import java.util.List;

import static org.qubership.cloud.configserver.config.service.ConsulMigrationValidator.VALUE_SIZE_ALARM;

class ConsulMigrationValidatorTest {

    @Test
    void test_ConsulMigrationValidator_gettingBigValue_throwsException() {
        ConfigProfile configProfile = new ConfigProfile();
        configProfile.setProfile("default");
        configProfile.setApplication("test-app");
        ConfigProperty property1 = createConfigPropertyWithBigValue("my.lovely.key1");
        ConfigProperty property2 = createConfigPropertyWithBigValue("my.lovely.key2");
        ConfigProperty property3 = createConfigProperty("my.lovely.key3", "value");
        configProfile.setProperties(List.of(property1, property2, property3));

        ConsulMigrationValidator validator = new ConsulMigrationValidator();
        ConsulMigrationValidator.Result result = validator.validate(List.of(configProfile));

        Assertions.assertTrue(result.hasError());
        Assertions.assertEquals(2, result.getErrors().size());
        Assertions.assertTrue(result.getErrors().contains(String.format(VALUE_SIZE_ALARM, "my.lovely.key1", "default")));
        Assertions.assertTrue(result.getErrors().contains(String.format(VALUE_SIZE_ALARM, "my.lovely.key2", "default")));
    }

    private ConfigProperty createConfigProperty(String key, String value) {
        ConfigProperty property = new ConfigProperty();
        property.setKey(key);
        property.setValue(value);
        return property;
    }

    private ConfigProperty createConfigPropertyWithBigValue(String key) {
        String value = RandomStringUtils.randomAlphabetic(ConsulMigrationValidator.CONSUL_VALUE_SIZE_RESTRICTION_UPPER_LIMIT_BYTES * 2);
        return createConfigProperty(key, value);
    }
}