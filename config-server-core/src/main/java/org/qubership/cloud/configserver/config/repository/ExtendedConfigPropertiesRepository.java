package org.qubership.cloud.configserver.config.repository;

import org.qubership.cloud.configserver.config.ConfigProfile;

import java.util.List;

public interface ExtendedConfigPropertiesRepository {
    void deleteAll(List<ConfigProfile> configProfiles);
    void deleteAll();
    List<ConfigProfile> findAll();
    List<ConfigProfile> findByApplicationAndProfile(String serviceName, String profile);
    ConfigProfile save(ConfigProfile configProfile);
    void deleteProperties(ConfigProfile configProfile, List<String> propertiesToDelete);
}
