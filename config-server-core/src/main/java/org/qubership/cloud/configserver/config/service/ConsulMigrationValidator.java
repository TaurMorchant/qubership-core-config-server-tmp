package org.qubership.cloud.configserver.config.service;

import org.qubership.cloud.configserver.config.ConfigProfile;
import org.qubership.cloud.configserver.config.ConfigProperty;
import lombok.Getter;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class ConsulMigrationValidator {
    public static final int CONSUL_VALUE_SIZE_RESTRICTION_UPPER_LIMIT_BYTES = 512 * 1024;
    public static final String VALUE_SIZE_ALARM = "Property %s from profile %s does not fit Consul requirements for value size. Value size must be < 512 KB";

    public Result validate(List<ConfigProfile> allConfigsPG) {
        Result result = new Result();
        result.errors.addAll(checkValueSize(allConfigsPG));

        return result;
    }

    private Collection<String> checkValueSize(List<ConfigProfile> allConfigsPG) {
        return allConfigsPG.stream()
                .flatMap(configProfile -> configProfile.getProperties()
                        .stream()
                        .filter(configProperty -> isNotEmpty(configProperty) && isGreaterLimit(configProperty))
                        .map(configProperty -> String.format(VALUE_SIZE_ALARM, configProperty.getKey(), configProfile.getProfile())))
                .collect(Collectors.toList());
    }

    private boolean isGreaterLimit(ConfigProperty configProperty) {
        return configProperty.getValue().getBytes(StandardCharsets.UTF_8).length >= CONSUL_VALUE_SIZE_RESTRICTION_UPPER_LIMIT_BYTES;
    }

    private boolean isNotEmpty(ConfigProperty configProperty) {
        return configProperty.getValue() != null && !configProperty.getValue().isEmpty();
    }

    @Getter
    public class Result {
        Set<String> errors = new HashSet<>();

        public boolean hasError() {
            return !errors.isEmpty();
        }
    }
}
