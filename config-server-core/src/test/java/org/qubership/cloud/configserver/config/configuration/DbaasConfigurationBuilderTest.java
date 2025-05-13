package org.qubership.cloud.configserver.config.configuration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.core.env.Environment;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.qubership.cloud.configserver.config.configuration.DbaasConfigurationBuilder.DB_CLASSIFIER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

/**
 * @author dmol0615
 * Date: 26.10.2018
 * Time: 14:15
 */
@RunWith(MockitoJUnitRunner.class)
public class DbaasConfigurationBuilderTest {

    private static final String MICROSERVICE_NAME = "config-server";
    private static final String LOCAL_DEV_NAMESPACE_ENV_KEY = "LOCALDEV_NAMESPACE";
    private static final String ATTACH_TO_CLOUD_DB_ENV_KEY = "attachToCloudDB";
    private static final String LOCAL_DEV_NAMESPACE = "localDevNamespace";

    @Mock
    private Environment environment;

    private Map<String, String> properties = new HashMap<>();

    @Before
    public void setUp() throws Exception {
        properties.put("spring.application.name", MICROSERVICE_NAME);
        properties.put(LOCAL_DEV_NAMESPACE_ENV_KEY, LOCAL_DEV_NAMESPACE);
        when(environment.getProperty(anyString())).thenAnswer((Answer<String>) invocationOnMock -> {
            String propertyKey = invocationOnMock.getArgument(0);
            return properties.get(propertyKey);
        });
    }

    @After
    public void tearDown() throws Exception {
        reset(environment);
        properties.clear();
    }

    @Test
    public void testCreateClassifier_withTenantId() throws Exception {
        DbaasConfigurationBuilder dbaasConfigurationBuilder = new DbaasConfigurationBuilder(environment);

        Map<String, Object> classifier = dbaasConfigurationBuilder.createClassifier(null);

        assertEquals(MICROSERVICE_NAME, classifier.get("microserviceName"));
        assertEquals("service", classifier.get("scope"));
        assertEquals(DB_CLASSIFIER, classifier.get("dbClassifier"));
        assertEquals(LOCAL_DEV_NAMESPACE, classifier.get("localdev"));
    }

    @Test
    public void testCreateClassifier_withoutTenantId() throws Exception {
        DbaasConfigurationBuilder dbaasConfigurationBuilder = new DbaasConfigurationBuilder(environment);
        String tenantId = UUID.randomUUID().toString();

        Map<String, Object> classifier = dbaasConfigurationBuilder.createClassifier(tenantId);

        assertEquals(MICROSERVICE_NAME, classifier.get("microserviceName"));
        assertEquals(tenantId, classifier.get("tenantId"));
        assertEquals(DB_CLASSIFIER, classifier.get("dbClassifier"));
        assertEquals(LOCAL_DEV_NAMESPACE, classifier.get("localdev"));
    }

    @Test
    public void testCreateClassifier_inCloudDbAttachMode() throws Exception {
        properties.put(ATTACH_TO_CLOUD_DB_ENV_KEY, "true");
        DbaasConfigurationBuilder dbaasConfigurationBuilder = new DbaasConfigurationBuilder(environment);

        Map<String, Object> classifier = dbaasConfigurationBuilder.createClassifier(null);

        assertNull(classifier.get("localdev"));
    }
}
