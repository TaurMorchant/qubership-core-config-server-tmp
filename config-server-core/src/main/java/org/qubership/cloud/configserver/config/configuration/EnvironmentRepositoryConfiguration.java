package org.qubership.cloud.configserver.config.configuration;

import org.qubership.cloud.configserver.config.repository.ConfigPropertiesRepository;
import org.qubership.cloud.configserver.config.repository.ConsulConfigPropertiesRepository;
import org.qubership.cloud.configserver.config.repository.DefaultEnvironmentRepository;
import org.qubership.cloud.configserver.config.repository.ExtendedConfigPropertiesRepository;
import org.qubership.cloud.configserver.config.repository.JpaConfigPropertiesRepository;
import org.qubership.cloud.configserver.config.service.ConsulService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.cloud.config.server.environment.SearchPathLocator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EnvironmentRepositoryConfiguration {
    @Bean
    public EnvironmentRepository environmentRepository() {
        return new DefaultEnvironmentRepository();
    }

    @Bean
    public SearchPathLocator searchPathLocator() {
        return null;
    }

    @Bean
    public ExtendedConfigPropertiesRepository extendedConfigPropertiesRepository(@Autowired(required = false) ConsulService consulService, ConfigPropertiesRepository configPropertiesRepository) {
        if (consulService != null) {
            return new ConsulConfigPropertiesRepository(consulService);
        }
        return new JpaConfigPropertiesRepository(configPropertiesRepository);
    }
}
