package org.qubership.cloud.configserver.config.repository;

import org.qubership.cloud.configserver.config.ConfigProperty;
import org.qubership.cloud.configserver.util.TestUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.qubership.config.UnitTestApplicationConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.config.environment.Environment;
import org.springframework.cloud.config.environment.PropertySource;
import org.springframework.cloud.config.server.environment.EnvironmentRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Collections;
import java.util.Map;

import static org.qubership.cloud.configserver.config.repository.DefaultEnvironmentRepository.CONFIG_PROPERTIES_DEFAULT_PROFILE_NAME;
import static org.qubership.cloud.configserver.config.repository.DefaultEnvironmentRepository.CONFIG_PROPERTIES_GLOBAL_APPLICATION_NAME;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {UnitTestApplicationConfig.class})
public class EnvironmentRepositoryTest {

    private final static String application = "ApplicationTest";
    private final static String profile = "ProfileTest";
    private final static String propertyKey = "propertyKey";
    private final static String globalPropertyValue = "globalPropertyValue";
    private final static String profilePropertyValue = "profilePropertyValue";
    private final static Map<String, ConfigProperty> globalProperties =
            Collections.singletonMap(propertyKey, new ConfigProperty(propertyKey, globalPropertyValue, false));
    private final static Map<String, ConfigProperty> profileProperties =
            Collections.singletonMap(propertyKey, new ConfigProperty(propertyKey, profilePropertyValue, false));

    @Autowired
    EnvironmentRepository environmentRepository;
    @Autowired
    private TestUtils utils;


    @After
    public void afterTest() {
        utils.dropCollection();
    }

    @Test
    public void findOne_ProfilePropertyExist_Test() {
        utils.createTestProfileInDB(application, profile, profileProperties, 1);

        utils.createTestProfileInDB(CONFIG_PROPERTIES_GLOBAL_APPLICATION_NAME,
                CONFIG_PROPERTIES_DEFAULT_PROFILE_NAME, globalProperties, 1);

        Environment env = environmentRepository.findOne(application, profile, null);
        PropertySource ps = env.getPropertySources().iterator().next();
        Map props = ps.getSource();

        String currentProperty = (String) props.get(propertyKey);
        assertThat(currentProperty, is(equalTo(profilePropertyValue)));
    }

    //
    @Test
    public void findOne_ProfilePropertyNotExist_Test() {
        utils.createTestProfileInDB(application, profile, Collections.emptyMap(), 1);
        utils.createTestProfileInDB(CONFIG_PROPERTIES_GLOBAL_APPLICATION_NAME,
                CONFIG_PROPERTIES_DEFAULT_PROFILE_NAME, globalProperties, 1);

        Environment env = environmentRepository.findOne(application, profile, null);
        PropertySource ps = env.getPropertySources().iterator().next();
        Map props = ps.getSource();

        String currentProperty = (String) props.get(propertyKey);
        assertThat(currentProperty, is(equalTo(globalPropertyValue)));
    }

    @Test
    public void findOne_ProfileNotExist_Test() {
        utils.createTestProfileInDB(CONFIG_PROPERTIES_GLOBAL_APPLICATION_NAME,
                CONFIG_PROPERTIES_DEFAULT_PROFILE_NAME, globalProperties, 1);

        Environment env = environmentRepository.findOne(application, profile, null);
        PropertySource ps = env.getPropertySources().iterator().next();
        Map props = ps.getSource();
        String currentProperty = (String) props.get(propertyKey);
        assertThat(currentProperty, is(equalTo(globalPropertyValue)));
    }

    @Test
    public void findOne_GlobalPropertyVersionOverride_Test() {
        utils.createTestProfileInDB(CONFIG_PROPERTIES_GLOBAL_APPLICATION_NAME,
                CONFIG_PROPERTIES_DEFAULT_PROFILE_NAME, globalProperties, 2);
        utils.createTestProfileInDB(application, profile, profileProperties, 1);

        Environment env = environmentRepository.findOne(application, profile, null);
        String version = env.getVersion();

        assertThat(version, is(equalTo("2")));
    }

    @Test
    public void findOne_LocalPropertyVersionOverride_Test() {
        utils.createTestProfileInDB(CONFIG_PROPERTIES_GLOBAL_APPLICATION_NAME,
                CONFIG_PROPERTIES_DEFAULT_PROFILE_NAME, globalProperties, 1);
        utils.createTestProfileInDB(application, profile, profileProperties, 2);

        Environment environment = environmentRepository.findOne(application, profile, null);
        String version = environment.getVersion();

        assertThat(version, is(equalTo("2")));
    }

    @Test
    public void findOne_mergePropertyOrder() {
        String profile = "profile1";

        utils.createTestProfileInDB(application, profile,
                Collections.singletonMap("key", new ConfigProperty("aa", "cc2", false)), 2);
        utils.createTestProfileInDB(application, profile,
                Collections.singletonMap("key", new ConfigProperty("aa", "cc", false)), 1);

        Environment env = environmentRepository.findOne(application, profile, null);
        PropertySource ps = env.getPropertySources().iterator().next();
        Map props = ps.getSource();

        String currentProperty = (String) props.get("aa");
        Assert.assertEquals("cc2", currentProperty);
    }
}
