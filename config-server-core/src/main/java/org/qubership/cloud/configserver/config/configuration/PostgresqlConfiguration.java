package org.qubership.cloud.configserver.config.configuration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactoryBuilder;
import org.apache.hc.core5.http.ssl.TLS;
import org.qubership.cloud.dbaas.client.DbaasClient;
import org.qubership.cloud.dbaas.client.DbaasClientImpl;
import org.qubership.cloud.dbaas.client.config.EnableServiceDbaasPostgresql;
import org.qubership.cloud.restclient.MicroserviceRestClient;
import org.qubership.cloud.restclient.resttemplate.MicroserviceRestTemplate;
import org.qubership.cloud.security.core.utils.tls.TlsUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.support.BasicAuthenticationInterceptor;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import javax.sql.DataSource;
import java.util.HashMap;

@Configuration
// vaultTemplate will be provided by SpringVaultClientConfiguration
@EnableServiceDbaasPostgresql
@EnableJpaRepositories("org.qubership.cloud.configserver.config.repository")
public class PostgresqlConfiguration {

    private static final Logger log = LoggerFactory.getLogger(PostgresqlConfiguration.class);
    private final String dbaasUsername;
    private final String dbaasPassword;
    private final String dbaasAgentHost;
    private final String microserviceName;

    public PostgresqlConfiguration(@Value("${dbaas.api.username:#{null}}") String dbaasUsername,
                                   @Value("${dbaas.api.password:#{null}}") String dbaasPassword,
                                   @Value("${dbaas.api.address}") String dbaasAgentHost,
                                   @Value("${spring.application.name}") String microserviceName) {
        this.dbaasUsername = dbaasUsername;
        this.dbaasPassword = dbaasPassword;
        this.dbaasAgentHost = dbaasAgentHost;
        this.microserviceName = microserviceName;
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dbaasPostgresDataSource) {
        LocalContainerEntityManagerFactoryBean em
                = new LocalContainerEntityManagerFactoryBean();
        em.setDataSource(dbaasPostgresDataSource);
        em.setPackagesToScan("org.qubership.cloud.configserver.config");

        JpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        em.setJpaVendorAdapter(vendorAdapter);

        return em;
    }

    @Bean
    public DbaasClient dbaasClient(@Autowired(required = false) @Qualifier("dbaasClientDefaultRetryTemplate") RetryTemplate retryTemplate) {
        RestTemplate restTemplate = new RestTemplate();
        final SSLConnectionSocketFactory sslSocketFactory = SSLConnectionSocketFactoryBuilder.create()
                .setSslContext(TlsUtils.getSslContext())
                .setTlsVersions(TLS.V_1_1, TLS.V_1_2, TLS.V_1_3)
                .build();
        final HttpClientConnectionManager cm = PoolingHttpClientConnectionManagerBuilder.create()
                .setSSLSocketFactory(sslSocketFactory)
                .build();
        HttpClient httpClient = HttpClients.custom()
                .setConnectionManager(cm)
                .build();
        restTemplate.setRequestFactory(new HttpComponentsClientHttpRequestFactory(httpClient));
        restTemplate.getInterceptors().add((httpRequest, bytes, clientHttpRequestExecution) -> {
            ObjectMapper objectMapper = new ObjectMapper();
            HashMap<String, Object> body = objectMapper.readValue(bytes, new TypeReference<HashMap<String, Object>>() {
            });
            body.put("originService", microserviceName);
            bytes = objectMapper.writeValueAsBytes(body);
            httpRequest.getHeaders().setContentLength(bytes.length);
            return clientHttpRequestExecution.execute(httpRequest, bytes);
        });
        if (!StringUtils.isEmpty(this.dbaasUsername) && !StringUtils.isEmpty(this.dbaasPassword)) {
            restTemplate.getInterceptors().add(new BasicAuthenticationInterceptor(this.dbaasUsername, this.dbaasPassword));
        }
        MicroserviceRestClient microserviceRestClient = new MicroserviceRestTemplate(restTemplate);
        DbaasClientImpl dbaasClient = new DbaasClientImpl(microserviceRestClient, retryTemplate, this.dbaasAgentHost);
        log.info("Created dbaasClient bean {}", dbaasClient);
        return dbaasClient;
    }

    @Bean
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        return transactionManager;
    }
}