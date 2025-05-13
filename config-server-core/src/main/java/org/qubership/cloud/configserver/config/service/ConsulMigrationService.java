package org.qubership.cloud.configserver.config.service;

import org.qubership.cloud.configserver.config.ConfigProfile;
import org.qubership.cloud.configserver.config.repository.ConfigPropertiesRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Set;

@Slf4j
public class ConsulMigrationService {
    public static final String NEED_MIGRATION_Q = "select m from consul_migrated order by m limit 1";

    private final String namespace;
    private final ConfigPropertiesRepository pgRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ConsulService consulService;

    public ConsulMigrationService(String namespace, ConfigPropertiesRepository pgRepository, JdbcTemplate jdbcTemplate, ConsulService consulService) {
        this.namespace = namespace;
        this.pgRepository = pgRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.consulService = consulService;
    }

    public void migrateToConsul() {
        log.info("Start data migration to Consul");
        List<ConfigProfile> allConfigsPG = pgRepository.findAll();
        log.info("Found {} profiles", allConfigsPG.size());

        log.info("Check Consul availability...");
        if (!consulService.isConsulAvailable()) {
            log.error("Can not migrate data to Consul");
            return;
        }
        log.info("Consul is available");

        log.info("Start validating profiles");
        ConsulMigrationValidator validator = new ConsulMigrationValidator();
        ConsulMigrationValidator.Result validationResult = validator.validate(allConfigsPG);

        if (validationResult.hasError()) {
            throwValidationException(validationResult.getErrors());
        }

        log.info("Start migration");
        allConfigsPG.forEach(consulService::addConfigProfile);
        log.info("Successfully migrated to Consul");

        jdbcTemplate.update("update consul_migrated set m = true");
        log.info("End data migration to Consul");
    }

    private void throwValidationException(Set<String> errors) {
        throw new ConsulMigrationException(String.join(";\n", errors));
    }
}