package org.qubership.cloud.configserver.config.service;

import org.qubership.cloud.configserver.config.ConfigProfile;
import org.qubership.cloud.configserver.config.ConfigProperty;
import com.orbitz.consul.Consul;
import com.orbitz.consul.ConsulException;
import com.orbitz.consul.model.kv.ImmutableOperation;
import com.orbitz.consul.model.kv.Operation;
import com.orbitz.consul.model.kv.Value;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static org.qubership.cloud.configserver.config.repository.DefaultEnvironmentRepository.CONFIG_PROPERTIES_GLOBAL_APPLICATION_NAME;
import static com.orbitz.consul.model.kv.Verb.DELETE;
import static com.orbitz.consul.model.kv.Verb.SET;

@Slf4j
public class ConsulService {
    public static final int CONSULT_TX_OPERATION_LIMIT = 64; // this limit is hardcoded in consul :( https://github.com/hashicorp/consul/issues/3917
    public static final int TXN_MAX_REQ_LEN = 512 * 1024; // 512 KB https://developer.hashicorp.com/consul/docs/agent/config/config-files#txn_max_req_len
    public static final String CONSUL_CONFIG_PREFIX = "config";
    public static final String DEFAULT_PROFILE_NAME = "default";
    public static final String CONSUL_GLOBAL_APPLICATION_NAME = "application";
    public static final String PROPERTIES_SOURCE = "consul";

    private static final UUID NIL_UUID = UUID.fromString("00000000-0000-0000-0000-000000000000");

    private final Consul consulClient;
    private final String namespace;

    public ConsulService(Consul consulClient, String namespace) {
        this.consulClient = consulClient;
        this.namespace = namespace;
    }

    public boolean isConsulAvailable() {
        if (consulClient == null) {
            return false;
        }
        try {
            consulClient.statusClient().getLeader();
        } catch (ConsulException ex) {
            log.error("Consul is not available", ex);
            return false;
        }
        return true;
    }

    public void addConfigProfile(ConfigProfile configProfile) {
        log.info("Adding config for {} with {} profile to Consul", configProfile.getApplication(), configProfile.getProfile());
        List<Operation> operation = new ArrayList<>();
        int batch = 0;
        int currentBatchSize = 0;

        for (ConfigProperty property : configProfile.getProperties()) {
            log.debug("Perform escape operations for {}: {}", property.getKey(), valueForLogger(property));
            String key = property.getKey();
            key = escape(key).replace(".", "/");
            key = formatKeyPrefix(configProfile) + key;
            String value = property.getValue() == null ? "" : property.getValue();
            Operation newOperation = ImmutableOperation.builder()
                    .verb(SET.toValue())
                    .key(key)
                    .value(value)
                    .build();

            int newOperationSize = calculateOperationSize(key, value);
            if (currentBatchSize + newOperationSize > TXN_MAX_REQ_LEN || batch == CONSULT_TX_OPERATION_LIMIT) {
                performTransaction(operation);
                operation = new ArrayList<>();
                batch = 0;
                currentBatchSize = 0;
            }

            operation.add(newOperation);
            currentBatchSize += newOperationSize;
            batch++;
            log.debug("Property {}: {} added to Consul successfully", key, valueForLogger(property));
        }
        if (!operation.isEmpty()) {
            performTransaction(operation);
        }
    }

    private void performTransaction(List<Operation> operation) {
        try {
            //calling API
            consulClient.keyValueClient().performTransaction(operation.toArray(new Operation[operation.size()]));
        } catch (Exception e) {
            log.error("Error during performing a transaction", e);
            throw new ConsulMigrationException("Consul Migration has failed Reason :- " + e.getMessage());
        }
    }

    public void deleteProperties(ConfigProfile configProfile, List<String> propertiesToDelete) {
        log.info("Deleting properties {} for {} with {} profile from Consul", propertiesToDelete, configProfile.getApplication(), configProfile.getProfile());
        List<Operation> operation = new ArrayList<>();
        int batch = 0;
        final String keyPrefix = formatKeyPrefix(configProfile);
        for (String key : propertiesToDelete) {
            key = keyPrefix + escape(key).replace(".", "/");
            operation.add(
                    ImmutableOperation.builder().verb(DELETE.toValue())
                            .key(key)
                            .build()
            );
            batch = batch + 1;
            if (batch == CONSULT_TX_OPERATION_LIMIT) {
                performTransaction(operation);
                operation = new ArrayList<>();
                batch = 0;
            }
        }
        if (!operation.isEmpty()) {
            performTransaction(operation);
        }
        log.info("Properties were successfully deleted from Consul");
    }

    public void deleteAll(List<ConfigProfile> configProfiles) {
        configProfiles.forEach(configProfile -> consulClient.keyValueClient().deleteKeys(formatKeyPrefix(configProfile)));
    }

    public void deleteAll() {
        consulClient.keyValueClient().deleteKeys(CONSUL_CONFIG_PREFIX + "/" + namespace);
    }

    public ConfigProfile getByApplicationAndProfile(String appName, String profileName) {
        String prefix = formatKeyPrefix(appName, profileName);
        List<Value> properties = consulClient.keyValueClient().getValues(prefix);

        return new ConfigProfile(NIL_UUID, appName, profileName, 0, toConfigProperties(properties, prefix));
    }

    public List<ConfigProfile> getAll() {
        String prefix = CONSUL_CONFIG_PREFIX + "/" + namespace;
        Map<String, List<Value>> propertiesByApp = consulClient.keyValueClient().getValues(prefix).
                stream().collect(Collectors.groupingBy(prop -> prop.getKey().split("/")[2]));
        return propertiesByApp.keySet().stream()
                .map(appName -> {
                    String[] appNameAndProfile = splitAppNameAndProfile(appName);
                    return new ConfigProfile(
                            NIL_UUID,
                            appNameAndProfile[0].equalsIgnoreCase(CONSUL_GLOBAL_APPLICATION_NAME) ? CONFIG_PROPERTIES_GLOBAL_APPLICATION_NAME : appNameAndProfile[0],
                            appNameAndProfile[1],
                            0,
                            toConfigProperties(propertiesByApp.get(appName), prefix + "/" + appName));
                })
                .collect(Collectors.toList());
    }

    private List<ConfigProperty> toConfigProperties(List<Value> properties, String prefix) {
        if (properties == null) {
            return Collections.emptyList();
        }
        return properties.stream()
                .filter(value -> !prefix.equals(value.getKey())) // key cannot equal to prefix. skipping such values
                .map(val -> new ConfigProperty(
                        unescape(cutPrefix(val.getKey(), prefix).replace("/", ".")),
                        val.getValueAsString().orElse(""),
                        false)
                )
                .collect(Collectors.toList());
    }

    private String[] splitAppNameAndProfile(String appNameWithProfile) {
        if (!appNameWithProfile.contains(",")) {
            return new String[]{appNameWithProfile, DEFAULT_PROFILE_NAME};
        }
        return appNameWithProfile.split(",");
    }

    String cutPrefix(String s, String prefix) {
        if (prefix.equals(s)) {
            log.warn("Property key cannot be equal to prefix {}", prefix);
            throw new IllegalArgumentException("Property key cannot be equal to prefix " + prefix);
        }
        int prefixSize = prefix.length();
        if (!prefix.endsWith("/")) {
            prefixSize += 1;
        }
        return s.substring(prefixSize);
    }

    private String formatKeyPrefix(String appName, String profileName) {
        String result = CONSUL_CONFIG_PREFIX + "/" + namespace + "/" + appName;
        if (!DEFAULT_PROFILE_NAME.equals(profileName)) {
            result += "," + profileName;
        }
        return result + "/";
    }

    private String formatKeyPrefix(ConfigProfile profile) {
        String appName = profile.getApplication();
        if (CONFIG_PROPERTIES_GLOBAL_APPLICATION_NAME.equals(appName)) {
            appName = CONSUL_GLOBAL_APPLICATION_NAME;
        }
        String result = CONSUL_CONFIG_PREFIX + "/" + namespace + "/" + appName;
        if (!DEFAULT_PROFILE_NAME.equals(profile.getProfile())) {
            result += "," + profile.getProfile();
        }
        return result + "/";
    }

    private final String[][] replacements = {
            {"/", "&sol;"},
            {"*", "&ast;"},
            {"?", "&quest;"},
            {"'", "&apos;"},
            {"%", "&percnt;"},
    };

    private String escape(String s) {
        for (String[] replacement : replacements) {
            s = s.replace(replacement[0], replacement[1]);
        }
        return s;
    }

    private String unescape(String s) {
        for (String[] replacement : replacements) {
            s = s.replace(replacement[1], replacement[0]);
        }
        return s;
    }

    private String valueForLogger(ConfigProperty property) {
        return property.getValue() == null ? "empty" : "masked-non-empty-value";
    }

    private int calculateOperationSize(String key, String value) {
        int keySize = key.getBytes(StandardCharsets.UTF_8).length;
        int valueSize = value.getBytes(StandardCharsets.UTF_8).length;
        return keySize + valueSize/3*4 + 100;  // The value is encoded in base64 this increases the size by 3/4, +100b for json struct\syntax
    }
}
