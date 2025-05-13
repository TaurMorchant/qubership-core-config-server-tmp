package org.qubership.cloud.configserver.config.controller;

import org.qubership.cloud.configserver.config.ConfigProperty;
import org.qubership.cloud.configserver.util.TestUtils;
import nl.altindag.log.LogCaptor;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.qubership.config.UnitTestApplicationConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {UnitTestApplicationConfig.class})
public class ConfigPropertiesControllerTest {

    @Autowired
    ConfigPropertiesController configPropertiesController;
    @Autowired
    private TestUtils utils;

    private final static String application = "ApplicationTest";
    private final static String profile = "ProfileTest";
    private final static String property1Key = "property1Key";
    private final static String property1Value = "property1Value";
    private final static String property2Key = "property2Key";
    private final static String property2Value = "property2Value";
    private final static String passwordKey = "propertyPassword";
    private final static Map<String, String> initProperties = Collections.singletonMap(property1Key, property1Value);
    private final static Map<String, String> addedProperties = Collections.singletonMap(property2Key, property2Value);
    private final static Map<String, String> replacedProperties = Collections.singletonMap(property1Key, property2Value);
    private final static Map<String, String> propertiesWithPassword = new HashMap<String, String>() {{
        put(passwordKey, property1Value);
        put(property2Key, property2Value);
    }};

    @After
    public void afterTest() {
        utils.dropCollection();
    }

    @Test
    public void addProperties_NoProfileExist_Test() throws InterruptedException {
        configPropertiesController.addProperties(application, profile, initProperties);
        assertResult(initProperties);
    }

    @Test
    public void addProperties_AddNewPropertyInMap_Test() throws InterruptedException {
        utils.createTestProfileInDB(application, profile, convertMapOfStringToMapOfProperties(initProperties), 1);
        configPropertiesController.addProperties(application, profile, addedProperties);
        Map resultProperties = new HashMap();
        resultProperties.putAll(initProperties);
        resultProperties.putAll(addedProperties);
        assertResult(resultProperties);
    }

    @Test
    public void addProperties_ReplaceProperty_Test() throws InterruptedException {
        utils.createTestProfileInDB(application, profile, convertMapOfStringToMapOfProperties(initProperties), 1);
        configPropertiesController.addProperties(application, profile, replacedProperties);
        assertResult(replacedProperties);
    }

    @Test
    public void addProperties_DeletePasswordFromLogs_Test() throws InterruptedException {
        String expected = String.format("Update properties for: %s/%s, properties: %s",
                application, profile, addedProperties);
        LogCaptor logCaptor = LogCaptor.forClass(ConfigPropertiesController.class);
        logCaptor.setLogLevelToInfo();
        configPropertiesController.addProperties(application, profile, propertiesWithPassword);
        assertThat(logCaptor.getInfoLogs()).containsExactly(expected);
    }

    @Test
    public void addProperties_WithWrongPropertyKeys_Test() throws InterruptedException {
        Map<String, String> addedProperties = new HashMap<>();
        final String conflictingKey11 = "a.b";
        final String conflictingKey12 = "a.b.c";
        final String conflictingKey21 = "f.g";
        final String conflictingKey22 = "f.g.h";
        final String conflictingKey3 = ".i.j";
        final String conflictingKey4 = ".k.l.m";

        final String value = "value";
        addedProperties.put(conflictingKey11, value);
        addedProperties.put(conflictingKey12, value);
        addedProperties.put(conflictingKey21, value);
        addedProperties.put(conflictingKey22, value);
        addedProperties.put(conflictingKey3, value);
        addedProperties.put(conflictingKey4, value);

        ResponseEntity<?> responseEntity = configPropertiesController.addProperties(application, profile, addedProperties);

        assertEquals(HttpStatus.BAD_REQUEST, responseEntity.getStatusCode());
        assertTrue(Objects.requireNonNull(responseEntity.getBody()).toString().contains(conflictingKey11 + " conflicts with " + conflictingKey12) || Objects.requireNonNull(responseEntity.getBody()).toString().contains(conflictingKey12 + " conflicts with " + conflictingKey11));
        assertTrue(Objects.requireNonNull(responseEntity.getBody()).toString().contains(conflictingKey21 + " conflicts with " + conflictingKey22) || Objects.requireNonNull(responseEntity.getBody()).toString().contains(conflictingKey22 + " conflicts with " + conflictingKey21));
        assertTrue(Objects.requireNonNull(responseEntity.getBody()).toString().contains(conflictingKey3 + " start with dot") && Objects.requireNonNull(responseEntity.getBody()).toString().contains(conflictingKey4 + " start with dot"));
    }

    private void assertResultByMapOfConfigProperty(Map<String, ConfigProperty> properties) {
        List<ConfigProperty> currentCollectionProperties =
                utils.findProfileCollection(application, profile);
        assertThat("DebugInfo: collection content: " + utils.getCollectionContent(),
                currentCollectionProperties, hasItems(utils.getPropertiesAsListObject(properties).toArray(new ConfigProperty[]{})));
    }

    private void assertResult(Map<String, String> properties) {
        assertResultByMapOfConfigProperty(convertMapOfStringToMapOfProperties(properties));
    }

    private Map<String, ConfigProperty> convertMapOfStringToMapOfProperties(Map<String, String> propertiesToConvert) {
        Map<String, ConfigProperty> mapOfConfigProperties = new HashMap<>(propertiesToConvert.size());
        for (Map.Entry<String, String> entry : propertiesToConvert.entrySet()) {
            mapOfConfigProperties.put(entry.getKey(), new ConfigProperty(entry.getKey(), entry.getValue(), false));
        }
        return mapOfConfigProperties;
    }
}
