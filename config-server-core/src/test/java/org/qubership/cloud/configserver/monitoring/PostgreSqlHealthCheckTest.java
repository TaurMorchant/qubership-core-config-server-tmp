package org.qubership.cloud.configserver.monitoring;

import org.qubership.cloud.configserver.config.ConfigProfile;
import org.qubership.cloud.configserver.config.ConfigProperty;
import org.qubership.cloud.configserver.config.repository.ConfigPropertiesRepository;
import org.qubership.cloud.configserver.config.repository.DefaultEnvironmentRepository;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.boot.actuate.health.Status;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PostgreSqlHealthCheckTest {

    private static final String TEST = "test";

    @InjectMocks
    private PostgreSqlDbHealthCheck postgreSqlDbHealthCheck;

    @Mock
    private ConfigPropertiesRepository configPropertiesRepository;

    @Test
    public void healthcheck_Ok() throws Exception {
        ConfigProfile newConfigProfile = new ConfigProfile();
        newConfigProfile.setApplication(DefaultEnvironmentRepository.CONFIG_PROPERTIES_GLOBAL_APPLICATION_NAME);
        newConfigProfile.setProfile(DefaultEnvironmentRepository.CONFIG_PROPERTIES_DEFAULT_PROFILE_NAME);
        newConfigProfile.setPropertiesFromMap(Collections.singletonMap(TEST, new ConfigProperty(TEST, TEST, false)));
        when(configPropertiesRepository.findByApplicationAndProfile(DefaultEnvironmentRepository.CONFIG_PROPERTIES_GLOBAL_APPLICATION_NAME,
                DefaultEnvironmentRepository.CONFIG_PROPERTIES_DEFAULT_PROFILE_NAME))
                .thenReturn(Arrays.asList(newConfigProfile));
        assertTrue(postgreSqlDbHealthCheck.health().getStatus() == Status.UP);
    }

    @Test
    public void healthcheck_Problem() throws Exception {
        when(configPropertiesRepository.findByApplicationAndProfile(anyString(), anyString()))
                .thenThrow(new DataAccessResourceFailureException(TEST));
        assertTrue(postgreSqlDbHealthCheck.health().getStatus().toString() == HealthCheckStatus.PROBLEM.name());
    }

    @Test
    public void healthcheck_Fatal() throws Exception {
        when(configPropertiesRepository.findByApplicationAndProfile(DefaultEnvironmentRepository.CONFIG_PROPERTIES_GLOBAL_APPLICATION_NAME,
                DefaultEnvironmentRepository.CONFIG_PROPERTIES_DEFAULT_PROFILE_NAME))
                .thenReturn(null);
        assertTrue(postgreSqlDbHealthCheck.health().getStatus() == Status.OUT_OF_SERVICE);
    }
}