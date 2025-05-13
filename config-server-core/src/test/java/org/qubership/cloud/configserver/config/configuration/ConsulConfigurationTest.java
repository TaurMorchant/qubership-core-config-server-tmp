package org.qubership.cloud.configserver.config.configuration;

import org.qubership.cloud.configserver.config.service.ConsulMigrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.qubership.cloud.configserver.config.service.ConsulMigrationService.NEED_MIGRATION_Q;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConsulConfigurationTest {
    ConsulConfiguration consulConfiguration = new ConsulConfiguration();

    @Test
    void migrateToConsul() {
        ConsulMigrationService consulMigrationService = mock(ConsulMigrationService.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);

        when(jdbcTemplate.queryForObject(NEED_MIGRATION_Q, Boolean.class)).thenReturn(true);
        InitializingBean initializingBean = consulConfiguration.migrateToConsul(consulMigrationService, jdbcTemplate);
        assertNull(initializingBean);

        when(jdbcTemplate.queryForObject(NEED_MIGRATION_Q, Boolean.class)).thenReturn(false);
        initializingBean = consulConfiguration.migrateToConsul(consulMigrationService, jdbcTemplate);
        assertNotNull(initializingBean);
    }
}