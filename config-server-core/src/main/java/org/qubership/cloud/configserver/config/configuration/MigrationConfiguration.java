package org.qubership.cloud.configserver.config.configuration;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.qubership.cloud.configserver.config.ConfigProfile;
import org.qubership.cloud.configserver.config.ConfigProperty;
import org.qubership.cloud.configserver.config.SpringUtility;
import org.qubership.cloud.configserver.config.repository.ConfigPropertiesRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.configuration.FluentConfiguration;
import org.qubership.cloud.smartclient.config.annotation.EnableFrameworkWebClient;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatusCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import javax.sql.DataSource;
import java.util.*;

@Slf4j
@Configuration
@Lazy(false)
@EnableFrameworkWebClient
public class MigrationConfiguration {
    public static final String NEED_BASELINE_MIGRATION_Q = "select m from baseline_migrated order by m limit 1";

    @Value("${consul.url:}")
    private String consulUrl;

    @Value("${BASELINE_PROJ:}")
    private String baselineProj;

    @Value("${baseline.fetch.properties:}")
    private List<String> baselineFetchProperties;

    //This bean runs first migration (thought spring) to init db scheme
    @Bean
    Flyway flyway(DataSource dataSource) {
        final Flyway flyway = Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .validateOnMigrate(false)
                .group(true)
                .locations("classpath:db/migration/postgresql")
                .target("1.001")
                .load();
        flyway.migrate();
        return flyway;
    }

    @Bean
    @DependsOn("flyway")
    FluentConfiguration fluentConfiguration(DataSource dataSource) {
        return Flyway.configure()
                .dataSource(dataSource)
                .baselineOnMigrate(true)
                .validateOnMigrate(false)
                .group(true)
                .locations("classpath:db/migration/postgresql", "classpath:org/qubership/cloud/configserver/config/migration");
    }

    //This bean runs other migrations after init all beans
    @Bean("flywayMigration")
    InitializingBean init(ApplicationContext applicationContext, FluentConfiguration fluentConfiguration) {
        return () -> {
            SpringUtility.setApplicationContext(applicationContext);
            Flyway load = fluentConfiguration.load();
            load.repair();
            load.migrate();
        };
    }

    @Bean("migrateBaseLineProps")
    @DependsOn("flywayMigration")
    InitializingBean migrateBaseLineProps(@Qualifier("m2mWebClient") WebClient webClient, ConfigPropertiesRepository repository, JdbcTemplate jdbcTemplate) {
        boolean isMigrated = jdbcTemplate.queryForObject(NEED_BASELINE_MIGRATION_Q, Boolean.class);
        return () -> {
            if (StringUtils.isBlank(baselineProj) || isMigrated) {
                log.info("Baseline is not set or already migrated. Skip migration.");
                return;
            }

            String responseJson = getConfigServerProperties(webClient, "global", "default");
            DocumentContext context = JsonPath.parse(responseJson);
            Map<String, String> baselineProps = context.read("$.propertySources[0].source", Map.class);
            List<ConfigProfile> globalProps = repository.findByApplicationAndProfile("global", "default");
            baselineFetchProperties.stream()
                    .filter(baselineProps::containsKey)
                    .forEach(prop ->
                            globalProps.forEach(configProfile -> updateOrInsertProp(configProfile, prop, baselineProps.get(prop)))
                    );
            repository.saveAll(globalProps);
            jdbcTemplate.update("update baseline_migrated set m = true");
            log.info("Properties successfuly migrated from baseline.");
        };
    }

    protected String getConfigServerProperties(WebClient webClient, String app, String profile) {
        WebClient.ResponseSpec responseSpec = webClient.get()
                .uri("http://config-server." + baselineProj + ":8080/" + app + "/" + profile)
                .retrieve()
                .onStatus(
                        HttpStatusCode::isError,
                        response -> response.bodyToMono(String.class).map(Exception::new)
                );
        return responseSpec.bodyToMono(String.class).block();
    }

    private void updateOrInsertProp(ConfigProfile profile, String key, String value) {
        if (profile == null) {
            return;
        }
        Optional<ConfigProperty> prop = profile.getProperties().stream()
                .filter(Objects::nonNull)
                .filter(configProperty -> configProperty.getKey().equals(key))
                .findFirst();
        if (prop.isPresent()) {
            prop.get().setValue(value);
        } else {
            List<ConfigProperty> properties = profile.getProperties();
            if (properties == null || properties.isEmpty()) {
                properties = new ArrayList<>();
                profile.setProperties(properties);
            }
            properties.add(new ConfigProperty(key, value, false));
        }
    }
}
