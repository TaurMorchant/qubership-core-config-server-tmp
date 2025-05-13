package org.qubership.cloud.configserver.config.repository;

import org.qubership.cloud.configserver.config.ConfigProfile;
import org.qubership.cloud.configserver.encryption.EncryptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class DefaultEnvironmentRepository implements EnvironmentRepository {

    public static final String PROPERTY_SOURCE_POSTGRESQL = "postgresql";
    public static final String CONFIG_PROPERTIES_GLOBAL_APPLICATION_NAME = "global";
    public static final String CONFIG_PROPERTIES_DEFAULT_PROFILE_NAME = "default";

    @Autowired
    private EncryptionService encryptor;
    @Autowired
    private ConfigProfileProvider configProfileProvider;

    /**
     * NOTE! Intentionally do not use caching here. I'm expect caching on client side depending on client needs
     * <p>
     * According to https://cloud.spring.io/spring-cloud-config/spring-cloud-config.html#_environment_repository
     * Precedence rules for profiles are also the same as in a regular Boot application:
     * active profiles take precedence over defaults,
     * and if there are multiple profiles the last one wins (like adding entries to a Map).
     */
    @Override
    public Environment findOne(String application, String activeProfile, String label) {
        log.debug("Receive config request for: {}, {}, {}", application, activeProfile, label);

        String[] activeProfiles = StringUtils.commaDelimitedListToStringArray(activeProfile);

        AtomicInteger environmentVersion = new AtomicInteger(); // unknown
        List<ConfigProfile> globalProfiles = configProfileProvider.findByApplicationAndProfile(
                CONFIG_PROPERTIES_GLOBAL_APPLICATION_NAME, CONFIG_PROPERTIES_DEFAULT_PROFILE_NAME);

        Map<String, String> mergedProperties = new HashMap<>();
        globalProfiles.stream()
                .sorted(Comparator.comparingInt(ConfigProfile::getVersion))
                .forEach(globalConfigProfile -> {
                    log.debug("Found global profile: {}", globalConfigProfile);
                    mergedProperties.putAll(encryptor.getDecryptedPropertiesAsMap(globalConfigProfile.getPropertiesAsMap()));
                    environmentVersion.set(Math.max(environmentVersion.get(), globalConfigProfile.getVersion()));
                });

        // merge service specific properties, active profiles take precedence over defaults
        for (String profile : activeProfiles) { // if there are multiple profiles the last one wins
            configProfileProvider.findByApplicationAndProfile(application, profile)
                    .stream()
                    .sorted(Comparator.comparingInt(ConfigProfile::getVersion))
                    .forEach(configProfile -> {
                        log.debug("Apply specific active profile: {}", configProfile);
                        mergedProperties.putAll(encryptor.getDecryptedPropertiesAsMap(configProfile.getPropertiesAsMap()));
                        environmentVersion.set(Math.max(environmentVersion.get(), configProfile.getVersion()));
                    });
        }

        log.debug("Merged properties: {}, version: {}", mergedProperties.keySet(), environmentVersion);
        Environment environment = new Environment(application, activeProfiles, label, String.valueOf(environmentVersion.get()), null);
        environment.add(new PropertySource(configProfileProvider.getPropertiesSource(), mergedProperties));
        return environment;
    }
}
