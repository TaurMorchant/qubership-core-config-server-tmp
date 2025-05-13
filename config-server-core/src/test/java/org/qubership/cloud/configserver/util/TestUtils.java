package org.qubership.cloud.configserver.util;

import org.qubership.cloud.configserver.config.ConfigProfile;
import org.qubership.cloud.configserver.config.ConfigProperty;
import org.qubership.cloud.configserver.config.repository.ConfigPropertiesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
public class TestUtils {

    @Autowired
    private ConfigPropertiesRepository repository;

    public List<ConfigProperty> getPropertiesAsList(Map<String, ConfigProperty> propertiesAsMap) {
        List<ConfigProperty> result = new ArrayList<>();
        for (Map.Entry<String, ConfigProperty> property : propertiesAsMap.entrySet()) {
            result.add(ConfigProperty.builder()
                    .key(property.getValue().getKey())
                    .value(property.getValue().getValue())
                    .encrypted(property.getValue().isEncrypted())
                    .build()
            );
        }
        return result;
    }

    public List<ConfigProperty> getPropertiesAsListObject(Map<String, ConfigProperty> propertiesAsMap) {
        List<ConfigProperty> result = new ArrayList<>();
        for (Map.Entry<String, ConfigProperty> property : propertiesAsMap.entrySet()) {
            result.add(property.getValue());
        }
        return result;
    }

    public ConfigProfile constructConfigProfileObject(String application, String profile) {
        return ConfigProfile.builder()
                .application(application)
                .profile(profile)
                .build();
    }

    private ConfigProfile constructConfigProfileObject(String application, String profile, Map<String, ConfigProperty> properties, Integer version) {
        return ConfigProfile.builder()
                .application(application)
                .profile(profile)
                .properties(getPropertiesAsList(properties))
                .version(version)
                .build();
    }

    public String getCollectionContent() {
        List<ConfigProfile> all = repository.findAll();
        String result = "";
        Iterator it = all.iterator();
        while (it.hasNext()) {
            result += it.next() + ";\n";
        }
        return result;
    }

    public void createTestProfileInDB(String application, String profile, Map<String, ConfigProperty> properties, Integer version) {
        ConfigProfile template = constructConfigProfileObject(application, profile, properties, version);
        repository.save(template);
    }

    public void dropCollection() {
        repository.deleteAllInBatch();
    }

    public List<ConfigProperty> findProfileCollection(String application, String profile) {
        List<ConfigProperty> result = new ArrayList<>();
        List<ConfigProfile> byApplicationAndProfile = repository.findByApplicationAndProfile(application, profile);
        Iterator<ConfigProfile> resultSet = byApplicationAndProfile.iterator();
        if (resultSet.hasNext()) {
            resultSet.next().getProperties().forEach(propertyObject -> {
                String key = propertyObject.getKey();
                String value = propertyObject.getValue();
                boolean encrypted = propertyObject.isEncrypted();
                ConfigProperty currentProperty = new ConfigProperty(key, value, encrypted);
                result.add(currentProperty);
            });
        }
        return result;
    }
}
