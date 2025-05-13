package org.qubership.cloud.configserver.config.migration;

import org.qubership.cloud.configserver.config.ConfigProfile;
import org.qubership.cloud.configserver.config.ConfigProperty;
import org.qubership.cloud.configserver.config.SpringUtility;
import org.qubership.cloud.configserver.config.repository.ConfigPropertiesRepository;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.util.HashMap;
import java.util.Map;

@NoArgsConstructor
public class V1_005__SetIdpRoutesInsteadOfHolder extends BaseJavaMigration {

    private ConfigPropertiesRepository repository = SpringUtility.getBean(ConfigPropertiesRepository.class);

    @Override
    public void migrate(Context context) throws Exception {
        String identityProvider = System.getenv("IDENTITY_PROVIDER");

        //for internal gateway
        Map<String, ConfigProperty> internalProperties = new HashMap<>();
        internalProperties.put(
                "route./api/v1/identity-provider/**",
                new ConfigProperty("route./api/v1/identity-provider/**", "http://identity-provider:8080", false)
        );

        ConfigProfile internal = repository.findByApplicationAndProfile("be-gateway-internal", "internal").get(0);
        Map<String, ConfigProperty> propertiesAsMap = internal.getPropertiesAsMap();
        propertiesAsMap.putAll(internalProperties);
        internal.setPropertiesFromMap(internalProperties);
        repository.save(internal);

        //for private gateway
        Map<String, ConfigProperty> privateProperties = new HashMap<>();
        privateProperties.put("route./api/v1/identity-provider/**",
                new ConfigProperty("route./api/v1/identity-provider/**", "http://identity-provider:8080", false));

        ConfigProfile privateProf = repository.findByApplicationAndProfile("be-gateway-private", "private").get(0);
        propertiesAsMap = privateProf.getPropertiesAsMap();
        propertiesAsMap.putAll(privateProperties);
        privateProf.setPropertiesFromMap(privateProperties);
        repository.save(privateProf);

        //for public gateway
        Map<String, ConfigProperty> publicProperties = new HashMap<>();
        if (!StringUtils.isBlank(identityProvider) && identityProvider.equals("mitre")) {
            publicProperties.put("route./api/v1/identity-provider/static/**",
                    new ConfigProperty("route./api/v1/identity-provider/static/**", "http://identity-provider:8080/static", false));
            publicProperties.put("route./api/v1/identity-provider/authorize/**",
                    new ConfigProperty("route./api/v1/identity-provider/authorize/**", "http://identity-provider:8080/authorize", false));
            publicProperties.put("route./api/v1/identity-provider/login/**",
                    new ConfigProperty("route./api/v1/identity-provider/login/**", "http://identity-provider:8080/login", false));
            publicProperties.put("route./j_spring_security_check/**",
                    new ConfigProperty("route./j_spring_security_check/**", "http://identity-provider:8080/j_spring_security_check", false));
            publicProperties.put("route./authorize/**",
                    new ConfigProperty("route./authorize/**", "http://identity-provider:8080/authorize", false));
        }
        ConfigProfile publicProf = repository.findByApplicationAndProfile("be-gateway-public", "public").get(0);
        propertiesAsMap = privateProf.getPropertiesAsMap();
        propertiesAsMap.putAll(publicProperties);
        publicProf.setPropertiesFromMap(propertiesAsMap);
        repository.save(publicProf);
    }
}
