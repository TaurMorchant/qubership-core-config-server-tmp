package org.qubership.cloud.configserver.config.migration;

import org.qubership.cloud.configserver.config.ConfigProfile;
import org.qubership.cloud.configserver.config.ConfigProperty;
import org.qubership.cloud.configserver.config.SpringUtility;
import org.qubership.cloud.configserver.config.repository.ConfigPropertiesRepository;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@NoArgsConstructor
public class V1_020__AddHttpHeaderMaxSize extends BaseJavaMigration {
    private static final String HTTP_BUFFER_HEADER_MAX_SIZE = "http.buffer.header.max.size";
    private final ConfigPropertiesRepository repository = SpringUtility.getBean(ConfigPropertiesRepository.class);

    @Override
    public void migrate(Context context) throws Exception {
        String httpHeaderMazSize = Optional.ofNullable(System.getenv("HTTP_BUFFER_HEADER_MAX_SIZE")).orElse("10240");
        Map<String, ConfigProperty> httpHeaderProperties = new HashMap<>();
        httpHeaderProperties.put(HTTP_BUFFER_HEADER_MAX_SIZE, new ConfigProperty(
                HTTP_BUFFER_HEADER_MAX_SIZE, httpHeaderMazSize, false));

        ConfigProfile globalProfile
                = repository.findByApplicationAndProfile("global", "default").get(0);
        Map<String, ConfigProperty> propertiesAsMap = globalProfile.getPropertiesAsMap();
        propertiesAsMap.putAll(httpHeaderProperties);
        globalProfile.setPropertiesFromMap(propertiesAsMap);
        repository.save(globalProfile);
        log.info("HTTP buffer header Max size has been added to global profile: {} bytes", httpHeaderMazSize);
    }
}
