package org.qubership.cloud.configserver.config.migration;

import org.qubership.cloud.configserver.config.ConfigProfile;
import org.qubership.cloud.configserver.config.ConfigProperty;
import org.qubership.cloud.configserver.config.SpringUtility;
import org.qubership.cloud.configserver.config.repository.ConfigPropertiesRepository;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@NoArgsConstructor
public class V1_014__AllowedHeadersProperty extends BaseJavaMigration {

    private ConfigPropertiesRepository repository = SpringUtility.getBean(ConfigPropertiesRepository.class);
    private Map<String, ConfigProperty> mergedProperties = new HashMap<>();
    private ConfigProfile globalConfigProfile;

    @Override
    public void migrate(Context context) throws Exception {
        List<ConfigProfile> profiles = repository.findByApplicationAndProfile("global", "default");
        log.info("size: {}", profiles.size());
        boolean multipleGlobalProfiles = profiles.size() > 1;

        if (multipleGlobalProfiles) {
            profiles.stream().forEach(globalConfigProfile -> {
                log.info("Found global profile: {}", globalConfigProfile);
                mergedProperties.putAll(globalConfigProfile.getPropertiesAsMap());
            });
            repository.deleteAll(profiles);
        } else {
            globalConfigProfile = profiles.get(0);
            mergedProperties = globalConfigProfile.getPropertiesAsMap();
        }

        String allowedHeaders = System.getenv("ALLOWED_HEADERS");
        boolean allowedHeadersNotBlank = !StringUtils.isBlank(allowedHeaders);
        if (allowedHeadersNotBlank) {
            log.info("Allowed headers: {}", allowedHeaders);
            mergedProperties.put("headers.allowed",
                    new ConfigProperty("headers.allowed", allowedHeaders, false));
        }

        if (multipleGlobalProfiles) {
            ConfigProfile.ConfigProfileBuilder cpb = ConfigProfile.builder()
                    .application("global").profile("default").version(1);
            for (ConfigProperty property : mergedProperties.values()) {
                cpb.property(
                        new ConfigProperty(property.getKey(), property.getValue(), property.getEncrypted()));
            }
            globalConfigProfile = cpb.build();
            repository.save(globalConfigProfile);
            log.info("Merged global profile is saved.");
        } else if (allowedHeadersNotBlank) {
            globalConfigProfile.setPropertiesFromMap(mergedProperties);
            repository.save(globalConfigProfile);
        }
    }
}
