package org.qubership.cloud.configserver.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity(name = "config_profile")
public class ConfigProfile {
    public static final int DEFAULT_VERSION = 1;

    @Id
    @GeneratedValue
    private UUID id;
    protected String application;
    protected String profile = "default";
    protected int version = DEFAULT_VERSION;
    @Singular
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    protected List<ConfigProperty> properties = new ArrayList<>();

    public void setPropertiesFromMap(Map<String, ConfigProperty> source) {
        this.properties = new ArrayList<>(source.values());
    }

    public Map<String, ConfigProperty> getPropertiesAsMap() {
        Map<String, ConfigProperty> result = new HashMap<>(properties.size());
        for (ConfigProperty property : properties) {
            result.put(property.getKey(), property);
        }
        return result;
    }
}
