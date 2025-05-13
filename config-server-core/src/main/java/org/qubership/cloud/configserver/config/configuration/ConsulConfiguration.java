package org.qubership.cloud.configserver.config.configuration;

import com.google.common.net.HostAndPort;
import org.qubership.cloud.configserver.config.repository.ConfigPropertiesRepository;
import org.qubership.cloud.configserver.config.service.ConsulMigrationService;
import org.qubership.cloud.configserver.config.service.ConsulService;
import com.orbitz.consul.Consul;
import lombok.extern.slf4j.Slf4j;
import org.qubership.cloud.security.core.utils.tls.TlsUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.jdbc.core.JdbcTemplate;

import java.net.URL;

import static org.qubership.cloud.configserver.config.service.ConsulMigrationService.NEED_MIGRATION_Q;

@Slf4j
@Configuration
@Lazy(false)
@ConditionalOnProperty(value = "consul.enabled", havingValue = "true")
public class ConsulConfiguration {
    @Value("${spring.application.namespace}")
    private String namespace;
    @Value("${consul.url}")
    private URL consulUrl;
    @Value("${consul.token:}")
    private String consulToken;

    @Bean
    Consul client() {
        return Consul.builder()
                .withSslContext(TlsUtils.getSslContext())
                .withTokenAuth(consulToken)
                .withHttps("https".equals(consulUrl.getProtocol()))
                .withHostAndPort(HostAndPort.fromParts(consulUrl.getHost(), consulUrl.getPort()))
                .build();
    }

    @Bean
    ConsulService consulService(Consul consulClient) {
        return new ConsulService(consulClient, namespace);
    }

    @Bean
    ConsulMigrationService consulMigrationService(ConfigPropertiesRepository repository, JdbcTemplate jdbcTemplate, ConsulService consulService) {
        return new ConsulMigrationService(namespace, repository, jdbcTemplate, consulService);
    }

    @Bean("consulMigration")
    @DependsOn("migrateBaseLineProps")
    InitializingBean migrateToConsul(ConsulMigrationService consulMigrationService, JdbcTemplate jdbcTemplate) {
        boolean isMigrated = jdbcTemplate.queryForObject(NEED_MIGRATION_Q, Boolean.class);
        if (isMigrated) {
            log.debug("The data has already been migrated to Consul.");
            return null;
        }
        return consulMigrationService::migrateToConsul;
    }
}
