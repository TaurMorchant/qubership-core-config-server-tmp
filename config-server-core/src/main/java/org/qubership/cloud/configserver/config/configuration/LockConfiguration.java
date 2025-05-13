package org.qubership.cloud.configserver.config.configuration;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.jdbc.lock.DefaultLockRepository;
import org.springframework.integration.jdbc.lock.JdbcLockRegistry;
import org.springframework.integration.jdbc.lock.LockRepository;

import javax.sql.DataSource;
import java.util.concurrent.TimeUnit;


@Configuration
public class LockConfiguration {

    @Value("${config.server.lock.jdbc.ttl_sec:300}")
    private Long ttlSec;

    @Bean
    public DefaultLockRepository DefaultLockRepository(DataSource dataSource) {
        DefaultLockRepository rep = new DefaultLockRepository(dataSource);
        rep.setTimeToLive((int) TimeUnit.SECONDS.toMillis(ttlSec)); // time to live of a lock record in DB
        return rep;
    }

    @Bean
    public JdbcLockRegistry jdbcLockRegistry(LockRepository lockRepository) {
        return new JdbcLockRegistry(lockRepository);
    }
}
