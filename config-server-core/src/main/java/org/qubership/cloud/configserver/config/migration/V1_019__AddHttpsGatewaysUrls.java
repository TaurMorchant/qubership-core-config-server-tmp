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

@Slf4j
@NoArgsConstructor
public class V1_019__AddHttpsGatewaysUrls extends BaseJavaMigration {
    private static final String APIGATEWAY_URL_HTTPS = "apigateway.url-https";
    private static final String APIGATEWAY_INTERNAL_URL_HTTPS = "apigateway.internal.url-https";
    private static final String APIGATEWAY_PRIVATE_URL_HTTPS = "apigateway.private.url-https";
    private static final String APIGATEWAY_PUBLIC_URL_HTTPS = "apigateway.public.url-https";
    private final ConfigPropertiesRepository repository = SpringUtility.getBean(ConfigPropertiesRepository.class);

    @Override
    public void migrate(Context context) throws Exception {
        Map<String, ConfigProperty> httpsUrlsProperties = new HashMap<>();
        httpsUrlsProperties.put(APIGATEWAY_URL_HTTPS, new ConfigProperty(
                APIGATEWAY_URL_HTTPS, "https://internal-gateway-service:8443", false));
        httpsUrlsProperties.put(APIGATEWAY_INTERNAL_URL_HTTPS, new ConfigProperty(
                APIGATEWAY_INTERNAL_URL_HTTPS, "https://internal-gateway-service:8443", false));
        httpsUrlsProperties.put(APIGATEWAY_PRIVATE_URL_HTTPS, new ConfigProperty(
                APIGATEWAY_PRIVATE_URL_HTTPS, "https://private-gateway-service:8443", false));
        httpsUrlsProperties.put(APIGATEWAY_PUBLIC_URL_HTTPS, new ConfigProperty(
                APIGATEWAY_PUBLIC_URL_HTTPS, "https://public-gateway-service:8443", false));

        ConfigProfile globalProfile
                = repository.findByApplicationAndProfile("global", "default").get(0);
        Map<String, ConfigProperty> propertiesAsMap = globalProfile.getPropertiesAsMap();
        propertiesAsMap.putAll(httpsUrlsProperties);
        globalProfile.setPropertiesFromMap(propertiesAsMap);
        repository.save(globalProfile);
        log.info("HTTPS gateways URLs have been added to global profile");
    }
}
