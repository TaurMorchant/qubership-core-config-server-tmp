package org.qubership.cloud.configserver.config.repository;

import org.qubership.cloud.configserver.config.ConfigProfile;
import org.qubership.cloud.configserver.config.ConfigProperty;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JpaConfigPropertiesRepository implements ExtendedConfigPropertiesRepository {

    private final ConfigPropertiesRepository jpaConfigPropertiesRepository;

    public JpaConfigPropertiesRepository(ConfigPropertiesRepository jpaConfigPropertiesRepository) {
        this.jpaConfigPropertiesRepository = jpaConfigPropertiesRepository;
    }

    @Override
    public void deleteAll(List<ConfigProfile> configProfiles) {
        jpaConfigPropertiesRepository.deleteAll(configProfiles);
    }

    @Override
    public void deleteAll() {
        jpaConfigPropertiesRepository.deleteAll();
    }

    @Override
    public List<ConfigProfile> findAll() {
        return jpaConfigPropertiesRepository.findAll();
    }

    @Override
    public List<ConfigProfile> findByApplicationAndProfile(String serviceName, String profile) {
        return jpaConfigPropertiesRepository.findByApplicationAndProfile(serviceName, profile);
    }

    @Override
    public ConfigProfile save(ConfigProfile configProfile) {
        return jpaConfigPropertiesRepository.save(configProfile);
    }

    @Override
    public void deleteProperties(ConfigProfile configProfile, List<String> propertiesToDelete) {
        Map<String, ConfigProperty> updatedProperties = new HashMap<>(configProfile.getPropertiesAsMap());
        propertiesToDelete.forEach(updatedProperties.keySet()::remove);
        configProfile.setVersion(configProfile.getVersion() + 1);
        configProfile.setPropertiesFromMap(updatedProperties);
        save(configProfile);
    }
}
