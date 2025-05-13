package org.qubership.cloud.configserver.monitoring;

import org.qubership.cloud.configserver.config.ConfigProfile;
import org.qubership.cloud.configserver.config.repository.ConfigPropertiesRepository;
import org.qubership.cloud.configserver.config.repository.DefaultEnvironmentRepository;
import org.qubership.cloud.configserver.config.service.ConsulMigrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@ConditionalOnMissingBean(ConsulMigrationService.class)
public class PostgreSqlDbHealthCheck implements HealthIndicator {
    private static final String FIELD_DETAILS = "details";

    @Autowired
    private ConfigPropertiesRepository configPropertiesRepository;

    @Override
    public Health health() {

        Health.Builder healthCheck = Health.up();

        try {
            List<ConfigProfile> globalProfiles = configPropertiesRepository.findByApplicationAndProfile(
                    DefaultEnvironmentRepository.CONFIG_PROPERTIES_GLOBAL_APPLICATION_NAME,
                    DefaultEnvironmentRepository.CONFIG_PROPERTIES_DEFAULT_PROFILE_NAME);

            if (globalProfiles == null || globalProfiles.isEmpty()) {
                log.error("Config-server properties are empty. Check the state of PostgreSql.");
                healthCheck.outOfService()
                        .withDetail(FIELD_DETAILS, "Config-server properties are empty.");
            }

        } catch (DataAccessException e) {
            log.error("PostgreSql is unavailable: ", e);
            healthCheck.status(HealthCheckStatus.PROBLEM.name())
                    .withDetail(FIELD_DETAILS, "PostgreSql is not available and configs may be obsolete.");
        }

        return healthCheck.build();
    }
}
