package org.qubership.cloud.configserver.config.repository;

import org.qubership.cloud.configserver.config.ConfigProfile;
import org.qubership.cloud.configserver.config.service.ConsulService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

import static org.qubership.cloud.configserver.config.repository.DefaultEnvironmentRepository.CONFIG_PROPERTIES_GLOBAL_APPLICATION_NAME;
import static org.qubership.cloud.configserver.config.service.ConsulService.CONSUL_GLOBAL_APPLICATION_NAME;

@Slf4j
@Component
public class ConfigProfileProvider {
    @Value("${consul.url:}")
    private String consulUrl;
    @Value("${consul.enabled:false}")
    private boolean isConsulEnabled;
    private final ConfigPropertiesRepository repository;
    private final ConsulService consulService;

    public ConfigProfileProvider(ConfigPropertiesRepository repository, @Autowired(required = false) ConsulService consulService) {
        this.repository = repository;
        this.consulService = consulService;
    }

    public String getPropertiesSource() {
        if (!consulUrl.isEmpty() && consulService != null && consulService.isConsulAvailable()) {
            return ConsulService.PROPERTIES_SOURCE;
        }
        return DefaultEnvironmentRepository.PROPERTY_SOURCE_POSTGRESQL;
    }

    public List<ConfigProfile> findByApplicationAndProfile(String serviceName, String profile) {
        if (!isConsulEnabled) {
            return repository.findByApplicationAndProfile(serviceName, profile);
        } else {
            if (consulService == null || !consulService.isConsulAvailable()) {
                log.error("Can not find config profiles: consul client is null or Consul is not available");
                return Collections.emptyList();
            }
            if (CONFIG_PROPERTIES_GLOBAL_APPLICATION_NAME.equals(serviceName)) {
                serviceName = CONSUL_GLOBAL_APPLICATION_NAME;
            }

            return Collections.singletonList(consulService.getByApplicationAndProfile(serviceName, profile));
        }
    }
}
