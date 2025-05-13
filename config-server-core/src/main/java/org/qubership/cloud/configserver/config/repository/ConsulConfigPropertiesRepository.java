package org.qubership.cloud.configserver.config.repository;

import org.qubership.cloud.configserver.config.ConfigProfile;
import org.qubership.cloud.configserver.config.service.ConsulService;

import java.util.Collections;
import java.util.List;

public class ConsulConfigPropertiesRepository implements ExtendedConfigPropertiesRepository {

    private final ConsulService consulService;

    public ConsulConfigPropertiesRepository(ConsulService consulService) {
        this.consulService = consulService;
    }

    @Override
    public void deleteAll(List<ConfigProfile> configProfiles) {
        consulService.deleteAll(configProfiles);
    }

    @Override
    public void deleteAll() {
        consulService.deleteAll();
    }

    @Override
    public List<ConfigProfile> findAll() {
        return consulService.getAll();
    }

    @Override
    public List<ConfigProfile> findByApplicationAndProfile(String serviceName, String profile) {
        return Collections.singletonList(consulService.getByApplicationAndProfile(serviceName, profile));
    }

    @Override
    public ConfigProfile save(ConfigProfile configProfile) {
        consulService.addConfigProfile(configProfile);
        return consulService.getByApplicationAndProfile(configProfile.getApplication(), configProfile.getProfile());
    }

    @Override
    public void deleteProperties(ConfigProfile configProfile, List<String> propertiesToDelete) {
        consulService.deleteProperties(configProfile, propertiesToDelete);
    }
}
