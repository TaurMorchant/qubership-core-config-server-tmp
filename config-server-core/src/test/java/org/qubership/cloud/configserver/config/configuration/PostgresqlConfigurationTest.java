package org.qubership.cloud.configserver.config.configuration;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManagerFactory;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockserver.client.MockServerClient;
import org.mockserver.model.Header;
import org.mockserver.model.HttpResponse;
import org.qubership.cloud.dbaas.client.DbaasClient;
import org.qubership.cloud.dbaas.client.entity.database.PostgresDatabase;
import org.qubership.cloud.dbaas.client.entity.database.type.PostgresDBType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.persistenceunit.DefaultPersistenceUnitManager;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.*;
import static org.mockserver.integration.ClientAndServer.startClientAndServer;
import static org.mockserver.model.HttpRequest.request;

public class PostgresqlConfigurationTest {

    private PostgresqlConfiguration postgresqlConfiguration;
    static String testDbaasUsername = "test-dbaas-username";
    static String testDbaasPassword = "test-dbaas-password";
    static String testDbaasAgentHost = "http://127.0.0.1:1080";
    static String testMicroserviceName = "test-ms-name";
    static String testNamespace = "test-namespace";

    static ObjectMapper mapper = new ObjectMapper();

    {
        postgresqlConfiguration = new PostgresqlConfiguration(
                testDbaasUsername,
                testDbaasPassword,
                testDbaasAgentHost,
                testMicroserviceName);
    }

    @Test
    public void entityManagerFactoryTest() {
        org.qubership.cloud.configserver.PostgresqlConfiguration dataPostgresqlConfiguration = new org.qubership.cloud.configserver.PostgresqlConfiguration();
        DataSource dataSource = dataPostgresqlConfiguration.testDataSource();
        LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactoryBean = postgresqlConfiguration.entityManagerFactory(dataSource);
        assertNotNull(localContainerEntityManagerFactoryBean);
        assertThat(localContainerEntityManagerFactoryBean, instanceOf(LocalContainerEntityManagerFactoryBean.class));
        assertNotNull(localContainerEntityManagerFactoryBean.getJpaVendorAdapter());
        Field internalPersistenceUnitManager = null;
        try {
            internalPersistenceUnitManager = localContainerEntityManagerFactoryBean.getClass().getDeclaredField("internalPersistenceUnitManager");
            internalPersistenceUnitManager.setAccessible(true);
            DefaultPersistenceUnitManager defaultPersistenceUnitManager = (DefaultPersistenceUnitManager) internalPersistenceUnitManager.get(localContainerEntityManagerFactoryBean);
            Field packagesToScan = defaultPersistenceUnitManager.getClass().getDeclaredField("packagesToScan");
            packagesToScan.setAccessible(true);
            String[] packagesToScanString = (String[]) packagesToScan.get(defaultPersistenceUnitManager);
            assertEquals("org.qubership.cloud.configserver.config", packagesToScanString[0]);
            assertSame(dataSource, localContainerEntityManagerFactoryBean.getDataSource());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }

    @Test
    public void transactionManagerTest() {
        EntityManagerFactory entityManagerFactory = Mockito.mock(EntityManagerFactory.class);
        PlatformTransactionManager transactionManager = postgresqlConfiguration.transactionManager(entityManagerFactory);
        assertNotNull(transactionManager);
        assertTrue(transactionManager instanceof JpaTransactionManager);
        assertEquals(entityManagerFactory, ((JpaTransactionManager) transactionManager).getEntityManagerFactory());
    }

    @Test
    public void testDbaasClientBodyInterceptor() {
        Map<String, Object> testClassifier = Map.of("scope", "service",
                "microserviceName", testMicroserviceName,
                "namespace", testNamespace);
        try (MockServerClient mockServerClient = startClientAndServer(1080)) {
            mockServerClient
                    .when(request())
                    .respond(r -> {
                                byte[] bytes = r.getBody().getRawBytes();
                                Map<String, Object> classifier = mapper.readValue(bytes, new TypeReference<>() {
                                });
                                assertTrue(classifier.entrySet().stream()
                                        .filter(entry -> testClassifier.containsKey(entry.getKey()))
                                        .allMatch(entry -> Objects.equals(entry.getValue(), testClassifier.get(entry.getKey()))));
                                assertNotNull(classifier.get("originService"));
                                String dbJson = mapper.writeValueAsString(new PostgresDatabase());
                                return new HttpResponse()
                                        .withStatusCode(200)
                                        .withHeader(new Header("Content-Type", "application/json;"))
                                        .withBody(dbJson);
                            }
                    );
            DbaasClient dbaasClient = postgresqlConfiguration.dbaasClient(RetryTemplate.builder().noBackoff().build());
            PostgresDatabase database = dbaasClient.getOrCreateDatabase(PostgresDBType.INSTANCE, testNamespace, testClassifier);
            assertNotNull(database);
        }
    }
}
