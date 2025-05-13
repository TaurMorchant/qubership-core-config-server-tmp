package org.qubership.cloud.configserver.config.migration;

import org.qubership.cloud.configserver.config.ConfigProfile;
import org.qubership.cloud.configserver.config.SpringUtility;
import org.qubership.cloud.configserver.config.repository.ConfigPropertiesRepository;
import org.qubership.cloud.configserver.config.repository.ConsulConfigPropertiesRepository;
import org.qubership.cloud.configserver.config.repository.JpaConfigPropertiesRepository;
import org.qubership.cloud.configserver.config.service.ConsulService;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;

import java.util.HashMap;
import java.util.List;

@Slf4j
@NoArgsConstructor
public class V1_022__DeleteConfigServerSpecificProperties extends BaseJavaMigration {

    private static final String DEFAULT_PROFILE = "default";
    private final ConfigPropertiesRepository configPropertiesRepository = SpringUtility.getBean(ConfigPropertiesRepository.class);

    @Override
    public void migrate(Context context) throws Exception {
        log.info("starting V1_022__DeleteConfigServerSpecificProperties");

        List<String> defaultPropsToDelete = List.of(
                "spring.application.namespace",
                "spring.sleuth.sampler.percentage",
                "management.health.mongo.enabled",
                "ELASTIC_NODES");

        log.info("processing with JpaConfigPropertiesRepository");
        JpaConfigPropertiesRepository jpaConfigPropertiesRepository = new JpaConfigPropertiesRepository(configPropertiesRepository);
        List<ConfigProfile> profiles = jpaConfigPropertiesRepository.findByApplicationAndProfile("global", DEFAULT_PROFILE);
        profiles.forEach(configProfile -> jpaConfigPropertiesRepository.deleteProperties(configProfile, defaultPropsToDelete));

        HashMap<String, String> appToDelete = new HashMap<>();
        appToDelete.put("product-catalog-provider-backend", DEFAULT_PROFILE);
        appToDelete.put("be-gateway-private", "private");
        appToDelete.put("be-gateway-internal", "internal");
        appToDelete.put("be-gateway-public", "public");

        profiles = appToDelete.entrySet().stream()
                .flatMap(m -> jpaConfigPropertiesRepository.findByApplicationAndProfile(m.getKey(), m.getValue()).stream())
                .toList();
        jpaConfigPropertiesRepository.deleteAll(profiles);

        ConsulService consulService = null;
        try {
            consulService = SpringUtility.getBean(ConsulService.class);
        } catch (NoSuchBeanDefinitionException e) {
            log.info("there is no ConsulService bean");
        }
        if (consulService != null) {
            log.info("processing with ConsulConfigPropertiesRepository");
            ConsulConfigPropertiesRepository consulConfigPropertiesRepository = new ConsulConfigPropertiesRepository(consulService);
            profiles = consulConfigPropertiesRepository.findByApplicationAndProfile("application", DEFAULT_PROFILE);
            profiles.forEach(configProfile -> consulConfigPropertiesRepository.deleteProperties(configProfile, defaultPropsToDelete));

            profiles = appToDelete.entrySet().stream()
                    .flatMap(m -> consulConfigPropertiesRepository.findByApplicationAndProfile(m.getKey(), m.getValue()).stream())
                    .toList();
            consulConfigPropertiesRepository.deleteAll(profiles);
        }

        log.info("finished V1_022__DeleteConfigServerSpecificProperties");
    }
}
