package org.qubership.cloud.configserver.config.configuration;

import org.qubership.cloud.configserver.PostgresqlConfiguration;
import org.qubership.cloud.configserver.config.ConfigProfile;
import org.qubership.cloud.configserver.config.ConfigProperty;
import org.qubership.cloud.configserver.config.repository.ConfigPropertiesRepository;
import org.apache.commons.lang3.ArrayUtils;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;

import javax.sql.DataSource;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.qubership.cloud.configserver.config.configuration.MigrationConfiguration.NEED_BASELINE_MIGRATION_Q;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class MigrationConfigurationTest {

    private static final MigrationConfiguration migrationConfiguration = new MigrationConfiguration();
    private static final PostgresqlConfiguration postgresqlConfiguration = new PostgresqlConfiguration();
    private static Method fluentConfigurationMethod;

    @Autowired
    ApplicationContext applicationContext;

    @BeforeClass
    public static void beforeClass() {
        Class[] cArg = new Class[1];
        cArg[0] = DataSource.class;
        try {
            fluentConfigurationMethod = migrationConfiguration.getClass().getDeclaredMethod("fluentConfiguration", cArg);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
            Assert.fail();
        }
        fluentConfigurationMethod.setAccessible(true);
    }

    @Test
    public void flywayTest() {
        Class[] cArg = new Class[1];
        cArg[0] = DataSource.class;
        try {
            Method flywayMethod = migrationConfiguration.getClass().getDeclaredMethod("flyway", cArg);
            flywayMethod.setAccessible(true);
            Flyway flyway = (Flyway) flywayMethod.invoke(migrationConfiguration, postgresqlConfiguration.testDataSource());
            assertNotNull(flyway);
            assertThat(flyway, instanceOf(Flyway.class));
            assertEquals("classpath:db/migration/postgresql", flyway.getConfiguration().getLocations()[0].toString());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void initTest() {
        Class[] cArg = new Class[2];
        cArg[0] = ApplicationContext.class;
        cArg[1] = FluentConfiguration.class;
        try {
            Method initMethod = migrationConfiguration.getClass().getDeclaredMethod("init", cArg);
            initMethod.setAccessible(true);
            FluentConfiguration fluentConfiguration = (FluentConfiguration) fluentConfigurationMethod.invoke(migrationConfiguration, postgresqlConfiguration.testDataSource());
            String[] expectedLocations = {"classpath:db/migration/postgresql", "classpath:org/qubership/cloud/configserver/config/migration"};
            assertEquals(2, fluentConfiguration.getLocations().length);
            ArrayUtils.contains(expectedLocations, fluentConfiguration.getLocations()[0].toString());
            ArrayUtils.contains(expectedLocations, fluentConfiguration.getLocations()[1].toString());
            InitializingBean initializingBean = (InitializingBean) initMethod.invoke(migrationConfiguration, applicationContext, fluentConfiguration);
            assertNotNull(initializingBean);
            assertThat(initializingBean, instanceOf(InitializingBean.class));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void migrateBaseLineProps() throws Exception {
        MigrationConfiguration migrationConfiguration = spy(new MigrationConfiguration());
        ReflectionTestUtils.setField(migrationConfiguration, "baselineProj", "test-proj");
        ReflectionTestUtils.setField(migrationConfiguration, "baselineFetchProperties", Arrays.asList("tenant.default.id", "bss.tenant.default-id"));
        WebClient webClient = mock(WebClient.class);
        ConfigPropertiesRepository repository = mock(ConfigPropertiesRepository.class);
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        InputStream inputStreamJson = getClass().getClassLoader().getResourceAsStream("response/config-server-global-default.json");
        String respJson = new BufferedReader(
                new InputStreamReader(inputStreamJson, StandardCharsets.UTF_8))
                .lines()
                .collect(Collectors.joining("\n"));

        when(jdbcTemplate.queryForObject(NEED_BASELINE_MIGRATION_Q, Boolean.class))
                .thenReturn(false);

        List<ConfigProfile> configs = new ArrayList<>();
        configs.add(ConfigProfile.builder().application("global").profile("default").properties(new ArrayList<>()).build());
        when(repository.findByApplicationAndProfile("global", "default")).thenReturn(configs);

        doReturn(respJson).when(migrationConfiguration).getConfigServerProperties(webClient, "global", "default");
        ArgumentCaptor<List<ConfigProfile>> propertiesCaptor = ArgumentCaptor.forClass(List.class);
        migrationConfiguration.migrateBaseLineProps(webClient, repository, jdbcTemplate).afterPropertiesSet();
        verify(repository).saveAll(propertiesCaptor.capture());
        List<ConfigProfile> profiles = propertiesCaptor.getValue();
        assertNotNull(profiles);
        assertEquals(1, profiles.size());

        List<ConfigProperty> properties = profiles.get(0).getProperties();
        assertFalse(properties.isEmpty());
        assertEquals(2, properties.size());
        assertEquals("tenant.default.id", properties.get(0).getKey());
        assertEquals("tenant.default.id.val", properties.get(0).getValue());
        assertEquals("bss.tenant.default-id", properties.get(1).getKey());
        assertEquals("bss.tenant.default-id.val", properties.get(1).getValue());

        when(jdbcTemplate.queryForObject(NEED_BASELINE_MIGRATION_Q, Boolean.class))
                .thenReturn(true);
        migrationConfiguration.migrateBaseLineProps(webClient, repository, jdbcTemplate).afterPropertiesSet();
        verify(repository, times(1)).saveAll(any());
    }
}
