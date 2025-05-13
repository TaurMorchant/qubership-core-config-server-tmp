package org.qubership.cloud.configserver.config.configuration;

import org.qubership.cloud.restclient.MicroserviceRestClientFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {


    /*
    Spring Boot 3 significantly changed the trailing slash matching configuration
    option. This option determines whether or not to treat a URL with a trailing slash
    the same as a URL without one. Previous versions of Spring Boot set this option
    to true by default. This meant that a controller would match both
    “GET /some/greeting” and “GET /some/greeting/” by default.
    We set the old behaviour.
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.setUseTrailingSlashMatch(true);
    }

    @Bean("simpleMicroserviceRestClientFactory")
    public MicroserviceRestClientFactory getMicroserviceRestClientFactory(
            @Value("${rest-client.pool.max-idle-time-sec:20}") int maxIdleTime,
            @Value("${rest-client.pool.pending-acquire-timeout-sec:30}") int pendingAcquireTimeout,
            @Value("${rest-client.pool.evict-in-background-sec:120}") int evictInBackground) {
        return new MicroserviceWebClientFactory(maxIdleTime, pendingAcquireTimeout, evictInBackground);
    }
}
