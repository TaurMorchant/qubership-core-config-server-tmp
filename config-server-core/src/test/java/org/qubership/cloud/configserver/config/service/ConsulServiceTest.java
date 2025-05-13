package org.qubership.cloud.configserver.config.service;

import com.google.common.net.HostAndPort;
import org.junit.jupiter.api.*;
import org.qubership.cloud.configserver.config.ConfigProfile;
import org.qubership.cloud.configserver.config.ConfigProperty;
import com.orbitz.consul.Consul;
import com.orbitz.consul.config.CacheConfig;
import com.orbitz.consul.config.ClientConfig;
import com.orbitz.consul.model.kv.Value;
import org.junit.Assert;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.qubership.cloud.configserver.config.service.ConsulService.CONSUL_CONFIG_PREFIX;

//todo vlla test temporary disabled - need to analyze reason of failure
@Disabled
public class ConsulServiceTest {

    private Consul client;

    private ConsulService consulService;

    private static final String NAMESPACE = "test-ns";

    public GenericContainer<?> consulContainer =
            new GenericContainer<>("hashicorp/consul:1.8.9")
                    .withCommand("agent", "-dev", "-client", "0.0.0.0", "--enable-script-checks=true")
                    .withExposedPorts(8500)
                    .waitingFor(new HostPortWaitStrategy().forPorts(8500));

    protected static HostAndPort defaultClientHostAndPort;

    @BeforeEach
    public void init() {
        consulContainer.start();

        defaultClientHostAndPort = HostAndPort.fromParts(consulContainer.getHost(), consulContainer.getFirstMappedPort());

        client = Consul.builder()
                .withHostAndPort(defaultClientHostAndPort)
                .withClientConfiguration(new ClientConfig(CacheConfig.builder().withWatchDuration(Duration.ofSeconds(1)).build()))
                .withReadTimeoutMillis(Duration.ofSeconds(2).toMillis())
                .withWriteTimeoutMillis(Duration.ofMillis(500).toMillis())
                .build();
        consulService = new ConsulService(client, NAMESPACE);
    }

    @AfterEach
    public void afterEach() {
        consulContainer.stop();
    }

    @Test
    void setFine() {
        ConfigProfile configProfile = new ConfigProfile();
        configProfile.setProfile("specific");
        configProfile.setApplication("test-app");

        List<ConfigProperty> properties = Arrays.asList(
                new ConfigProperty("my.lovely.key", "my-lovely.value", false),
                new ConfigProperty("my.lovely.key1", "my-lovely.value1", false),
                new ConfigProperty("my.lovely.key2", "my-lovely.value2", true)
        );
        configProfile.setProperties(properties);

        consulService.addConfigProfile(configProfile);

        ConfigProfile configProfile2 = new ConfigProfile();
        configProfile2.setProfile("default");
        configProfile2.setApplication("global");

        List<ConfigProperty> properties2 = Arrays.asList(
                new ConfigProperty("my.lovely.key3", "my-lovely.value3", true),
                new ConfigProperty("my.lovely.key4", "my-lovely.value4", false),
                new ConfigProperty("my.lovely.key5", "my-lovely.value5", false)
        );
        configProfile2.setProperties(properties2);

        consulService.addConfigProfile(configProfile2);

        ConfigProfile dataFromConsul = consulService.getByApplicationAndProfile("test-app", "specific");

        Assertions.assertEquals(configProfile.getApplication(), dataFromConsul.getApplication());
        Assertions.assertEquals(configProfile.getProfile(), dataFromConsul.getProfile());
        Assertions.assertFalse(dataFromConsul.getProperties().isEmpty());

        Assertions.assertTrue(configProfile.getProperties().stream()
                .allMatch(configProperty ->
                        dataFromConsul.getProperties().stream().anyMatch(fromConsul -> {
                            if (fromConsul.getKey().equals(configProperty.getKey())) {
                                return configProperty.getValue().equals(fromConsul.getValue());
                            }
                            return false;
                        })
                )
        );

        List<ConfigProfile> allConfigProfile = consulService.getAll();

        Assertions.assertEquals(2, allConfigProfile.size());

        Assertions.assertTrue(Stream.of("global", "test-app").allMatch(prof ->
                        allConfigProfile.stream().anyMatch(confProf -> prof.equals(confProf.getApplication()))
                )
        );

        Assertions.assertTrue(Stream.of("specific", "default").allMatch(prof ->
                        allConfigProfile.stream().anyMatch(confProf -> prof.equals(confProf.getProfile()))
                )
        );

        Assertions.assertTrue(
                Stream.concat(configProfile.getProperties().stream(), configProfile.getProperties().stream())
                        .allMatch(configProperty ->
                                allConfigProfile.stream().anyMatch(configProfile1 ->
                                        configProfile1.getProperties().stream().anyMatch(fromConsul -> {
                                                    if (fromConsul.getKey().equals(configProperty.getKey())) {
                                                        return configProperty.getValue().equals(fromConsul.getValue());
                                                    }
                                                    return false;
                                                }
                                        )
                                )
                        )
        );
    }

    @Test
    void testOnConsulMigrationSuccess() {
        ConfigProfile configProfile = new ConfigProfile();
        configProfile.setProfile("default");
        configProfile.setApplication("test-app");
        ConfigProperty property = new ConfigProperty();
        property.setKey("my.lovely.key");
        property.setValue("my.lovely.value");
        configProfile.setProperties(Collections.singletonList(property));

        consulService.addConfigProfile(configProfile);

        Optional<Value> value = client.keyValueClient().getValue("config/test-ns/test-app/my/lovely/key");
        Assert.assertEquals(value.get().getValueAsString().orElse(null), property.getValue());
    }

    @Test
    void failureOnBigProperty() {
        ConfigProfile configProfile = new ConfigProfile();
        configProfile.setProfile("default");
        configProfile.setApplication("test-app");
        ConfigProperty property = new ConfigProperty();
        property.setKey("my.lovely.key");

        char[] chars = new char[ConsulMigrationValidator.CONSUL_VALUE_SIZE_RESTRICTION_UPPER_LIMIT_BYTES * 2];
        Arrays.fill(chars, 'a');

        property.setValue(new String(chars));
        configProfile.setProperties(Collections.singletonList(property));

        Assert.assertThrows(ConsulMigrationException.class, () -> {
            consulService.addConfigProfile(configProfile);
        });

        Optional<Value> value = client.keyValueClient().getValue("config/test-ns/test-app/my/lovely/key");
        Assert.assertFalse(value.isPresent());
    }

    @Test
    void skipValueForKeyEqualedToPrefix() {
        String propPath = CONSUL_CONFIG_PREFIX + "/" + NAMESPACE + "/test-app";
        client.keyValueClient().putValue(propPath, "hello world from prefix");
        client.keyValueClient().putValue(propPath + "/key", "hello world from prop");
        Optional<Value> value1 = client.keyValueClient().getValue(propPath);
        Assertions.assertTrue(value1.isPresent());
        Optional<Value> value2 = client.keyValueClient().getValue(propPath + "/key");
        Assertions.assertTrue(value2.isPresent());

        List<ConfigProfile> list = consulService.getAll();
        Assertions.assertEquals(1, list.get(0).getProperties().size());
        Assertions.assertEquals("hello world from prop", list.get(0).getProperties().get(0).getValue());

        Assertions.assertThrowsExactly(IllegalArgumentException.class,
                () -> consulService.cutPrefix("name", "name"));
    }

    @Test
    void testTrueWhenConsulIsAvailable() {
        ConfigProfile configProfile = new ConfigProfile();
        configProfile.setProfile("default");
        configProfile.setApplication("test-app");
        ConfigProperty property = new ConfigProperty();
        property.setKey("my.lovely.key");

        boolean flag = consulService.isConsulAvailable();
        Assert.assertEquals(true, flag);
    }

    @Test
    void testGetByApplicationAndProfile() {
        ConfigProfile configProfile = new ConfigProfile();
        configProfile.setProfile("default");
        configProfile.setApplication("test-app");
        ConfigProperty property = new ConfigProperty();
        property.setKey("my.lovely.key");
        property.setValue("my.lovely.value");
        configProfile.setProperties(Collections.singletonList(property));

        ConfigProfile configProfile1 = consulService.getByApplicationAndProfile(configProfile.getApplication(), configProfile.getProfile());

        Assert.assertEquals(configProfile.getProfile(), configProfile1.getProfile());

    }

    @Test
    void testDeleteProperties() {
        final String application = "test-app";
        final String profile1 = "default";
        final String profile2 = "prod";
        final String key1 = "my.lovely.key1";
        final String key2 = "my.lovely.key2";
        final String key3 = "my.lovely.key3";

        ConfigProfile configProfile1 = new ConfigProfile();
        configProfile1.setProfile(profile1);
        configProfile1.setApplication(application);
        ConfigProperty property1 = new ConfigProperty(key1, "my.lovely.value1", false);
        ConfigProperty property2 = new ConfigProperty(key2, "my.lovely.value2", false);
        ConfigProperty property3 = new ConfigProperty(key3, "my.lovely.value3", false);
        configProfile1.setProperties(List.of(property1, property2, property3));

        ConfigProfile configProfile2 = new ConfigProfile();
        configProfile2.setProfile(profile2);
        configProfile2.setApplication(application);
        ConfigProperty property4 = new ConfigProperty(key1, "my.lovely.value1", false);
        configProfile2.setProperties(Collections.singletonList(property4));

        consulService.addConfigProfile(configProfile1);
        Assertions.assertTrue(consulService.getByApplicationAndProfile(application, profile1).getPropertiesAsMap().keySet().containsAll(List.of(key1, key2, key3)));
        consulService.addConfigProfile(configProfile2);
        Assertions.assertTrue(consulService.getByApplicationAndProfile(application, profile2).getPropertiesAsMap().containsKey(key1));

        consulService.deleteProperties(configProfile1, List.of(key1, key3));
        ConfigProfile configProfile1FromConsul = consulService.getByApplicationAndProfile(application, profile1);
        Assertions.assertFalse(configProfile1FromConsul.getPropertiesAsMap().containsKey(key1));
        Assertions.assertFalse(configProfile1FromConsul.getPropertiesAsMap().containsKey(key3));
        Assertions.assertTrue(configProfile1FromConsul.getPropertiesAsMap().containsKey(key2));
        ConfigProfile configProfile2FromConsul = consulService.getByApplicationAndProfile(application, profile2);
        Assertions.assertTrue(configProfile2FromConsul.getPropertiesAsMap().containsKey(key1));

        consulService.deleteProperties(configProfile2, Collections.singletonList(key1));
        Assertions.assertTrue(consulService.getByApplicationAndProfile(application, profile2).getProperties().isEmpty());
    }

    @Test
    void testSaveSeveralBigProperties() {
        ConfigProfile configProfile = new ConfigProfile();
        configProfile.setProfile("default");
        configProfile.setApplication("test-app");
        ConfigProperty property1 = new ConfigProperty();
        ConfigProperty property2 = new ConfigProperty();
        ConfigProperty property3 = new ConfigProperty();
        property1.setKey("my.lovely.key1");
        property2.setKey("my.lovely.key2");
        property3.setKey("my.lovely.key3");

        char[] chars1 = new char[ConsulMigrationValidator.CONSUL_VALUE_SIZE_RESTRICTION_UPPER_LIMIT_BYTES / 2 - 1000];
        char[] chars2 = new char[ConsulMigrationValidator.CONSUL_VALUE_SIZE_RESTRICTION_UPPER_LIMIT_BYTES / 2 - 1000];
        char[] chars3 = new char[ConsulMigrationValidator.CONSUL_VALUE_SIZE_RESTRICTION_UPPER_LIMIT_BYTES / 2 - 1000];
        Arrays.fill(chars1, 'a');
        Arrays.fill(chars2, 'b');
        Arrays.fill(chars3, 'c');

        property1.setValue(new String(chars1));
        property2.setValue(new String(chars2));
        property3.setValue(new String(chars3));
        configProfile.setProperties(List.of(property1, property2, property3));

        Assertions.assertDoesNotThrow(() -> consulService.addConfigProfile(configProfile));
        Assertions.assertTrue(client.keyValueClient().getValue("config/test-ns/test-app/my/lovely/key1").isPresent());
        Assertions.assertTrue(client.keyValueClient().getValue("config/test-ns/test-app/my/lovely/key2").isPresent());
        Assertions.assertTrue(client.keyValueClient().getValue("config/test-ns/test-app/my/lovely/key3").isPresent());
    }
}