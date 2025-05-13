package org.qubership.cloud.configserver.config.migration;

import org.qubership.cloud.configserver.config.ConfigProfile;
import org.qubership.cloud.configserver.config.ConfigProperty;
import org.qubership.cloud.configserver.config.SpringUtility;
import org.qubership.cloud.configserver.config.repository.ConfigPropertiesRepository;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;


@Slf4j
@NoArgsConstructor
public class V1_002__InitProperties extends BaseJavaMigration {

    private ConfigPropertiesRepository repository = SpringUtility.getBean(ConfigPropertiesRepository.class);

    private static final String ELASTIC_URL_DEFAULT = "es-client-s-compute.elasticsearch-cluster:9300";
    private static final String INSTALLATION_NAME_RULES = "[{\"namespace\":null,\"microserviceName\":null,\"tenantId\":null,\"dbClassifier\":null, \"installationName\" : \"default\"}]";

    @Override
    public void migrate(Context context) {
        initConfigRepository(repository);
        initMitreIdpRoutes(repository);
        clearDefaultKeycloakCredentials(repository);
        changeKeycloakPropertiesName(repository);
        fixEmailsAndCustomerSpecifier(repository);
    }

    public void initConfigRepository(ConfigPropertiesRepository repository) {
        String namespace = System.getenv("CLOUD_NAMESPACE");
        String cloudPublicHost = System.getenv("CLOUD_PUBLIC_HOST");
        String cloudPort = Optional.ofNullable(System.getenv("CLOUD_API_PORT")).orElse("6443");
        String cloudProtocol = Optional.ofNullable(System.getenv("CLOUD_PROTOCOL")).orElse("https");
        String cloudHost = Optional.ofNullable(System.getenv("CLOUD_API_HOST")).orElse(cloudPublicHost);
        String cloudInternalHost = StringUtils.isBlank(System.getenv("CLOUD_PRIVATE_HOST")) ?
                cloudPublicHost : System.getenv("CLOUD_PRIVATE_HOST");
        String elasticSearch = getElasticSearchUrlOrDefault();
        String privateGatewayUrl = Optional.ofNullable(System.getenv("PRIVATE_GATEWAY_URL")).orElse(cloudProtocol + "://" + "private-gateway-" + namespace + "." + cloudInternalHost);
        String publicGatewayUrl = Optional.ofNullable(System.getenv("PUBLIC_GATEWAY_URL")).orElse(cloudProtocol + "://" + "public-gateway-" + namespace + "." + cloudPublicHost);
        String cloudServerUrl = cloudProtocol + "://" + cloudHost + ":" + cloudPort;

        ConfigProfile globalProfile = ConfigProfile.builder()
                .application("global")
                .profile("default")
                .version(1)
                .property(new ConfigProperty("openshift.namespace", namespace, false))
                .property(new ConfigProperty("openshift.server.url", cloudServerUrl, false))
                .property(new ConfigProperty("openshift.server.internal.url", cloudProtocol + "://" + cloudInternalHost + ":" + cloudPort, false))
                .property(new ConfigProperty("spring.application.namespace", namespace, false))
                .property(new ConfigProperty("idp.gateway.clientId", "gateway", false))
                .property(new ConfigProperty("idp.gateway.clientSecret", "52245dfa-8f00-11e6-ae22-56b6b6499611", false))
                .property(new ConfigProperty("idp.gateway.route", "/api/v1/identity-provider", false))
                .property(new ConfigProperty("apigateway.url", "http://internal-gateway-service:8080", false))
                .property(new ConfigProperty("apigateway.internal.url", "http://internal-gateway-service:8080", false))
                .property(new ConfigProperty("apigateway.private.url", "http://private-gateway-service:8080", false))
                .property(new ConfigProperty("apigateway.public.url", "http://public-gateway-service:8080", false))
                .property(new ConfigProperty("apigateway.external.private.url", privateGatewayUrl, false))
                .property(new ConfigProperty("apigateway.external.public.url", publicGatewayUrl, false))
                .property(new ConfigProperty("apigateway.routes.registration.url", "/api/v1/routes", false))
                .property(new ConfigProperty("spring.sleuth.sampler.percentage", "1", false))
                .property(new ConfigProperty("management.health.mongo.enabled", "false", false))
                .property(new ConfigProperty("ELASTIC_NODES", elasticSearch, false))
                .build();
        repository.save(globalProfile);
        log.trace("Inserted global profile: {}", globalProfile);

        ConfigProfile internalGatewayProfile = ConfigProfile.builder()
                .application("be-gateway-internal")
                .profile("internal")
                .version(1)
                .property(new ConfigProperty("route./api/v1/identity-provider/**", "http://identity-provider:8080", false))
                .property(new ConfigProperty("route./api/v1/tenant-manager/tenant-registrations/**", "http://tenant-manager:8080/api/v1/tenant-manager/tenant-registrations", false))
                .build();
        repository.save(internalGatewayProfile);
        log.trace("Inserted internal gateway profile: {}", internalGatewayProfile);

        ConfigProfile privateGatewayProfile = ConfigProfile.builder()
                .application("be-gateway-private")
                .profile("private")
                .version(1)
                .property(new ConfigProperty("route./api/v1/identity-provider/**", "http://identity-provider:8080", false))
                .property(new ConfigProperty("route./api/v1/config-server/**", "http://config-server:8080", false))
                .build();
        repository.save(privateGatewayProfile);
        log.trace("Inserted private gateway profile: {}", privateGatewayProfile);

        ConfigProfile publicGatewayProfile = ConfigProfile.builder()
                .application("be-gateway-public")
                .profile("public")
                .version(1)
                .build();
        repository.save(publicGatewayProfile);
        log.trace("Inserted public gateway profile: {}", publicGatewayProfile);

        ConfigProfile productCatalogProviderDefaultProfile = ConfigProfile.builder()
                .application("product-catalog-provider-backend")
                .profile("default")
                .version(1)
                .property(new ConfigProperty("spring.data.elasticsearch.cluster-nodes", elasticSearch, false))
                .build();
        repository.save(productCatalogProviderDefaultProfile);
        log.trace("Inserted product catalog profile: {}", productCatalogProviderDefaultProfile);

        ConfigProfile tenantManagerDefaultProfile = ConfigProfile.builder()
                .application("tenant-manager")
                .profile("default")
                .property(new ConfigProperty("installationNameRules", INSTALLATION_NAME_RULES, false))
                .property(new ConfigProperty("openshift.server.url", cloudServerUrl, false))
                .property(new ConfigProperty("tenant.registration.success_template", "Dear %s %s your request is in progress.<br> We will send you an email all information after request approval.<br> Thank you.", false))
                .property(new ConfigProperty("tenant.shoppingFrontend.templateName", "qubership-cloud-shopping-frontend", false))
                .property(new ConfigProperty("tenant.service.alias.template", "DEFAULT", false))
                .property(new ConfigProperty("default_credentials.tenant-manager.user", "tenant", false))
                .property(new ConfigProperty("default_credentials.tenant-manager.password", "tenant", false))
                .property(new ConfigProperty("default_credentials.tenant-manager.auth-db", "tenants", false))
                .property(new ConfigProperty("default_credentials.dbaas.auth-db", "", false))
                .build();

        repository.save(tenantManagerDefaultProfile);
        log.trace("Inserted tenant manager profile: {}", tenantManagerDefaultProfile);
    }

    public void initMitreIdpRoutes(ConfigPropertiesRepository repository) {
        String idp = System.getenv("IDENTITY_PROVIDER");
        if ("mitre".equals(idp)) {
            List<ConfigProperty> properties = new ArrayList<>(5);
            properties.add(new ConfigProperty(
                    "route./api/v1/identity-provider/static/**",
                    "http://identity-provider:8080/static",
                    false
            ));
            properties.add(new ConfigProperty(
                    "route./api/v1/identity-provider/authorize/**",
                    "http://identity-provider:8080/authorize",
                    false
            ));
            properties.add(new ConfigProperty(
                    "route./api/v1/identity-provider/login/**",
                    "http://identity-provider:8080/login",
                    false
            ));
            properties.add(new ConfigProperty(
                    "route./j_spring_security_check/**",
                    "http://identity-provider:8080/j_spring_security_check",
                    false
            ));
            properties.add(new ConfigProperty(
                    "route./authorize/**",
                    "http://identity-provider:8080/authorize",
                    false
            ));

            ConfigProfile profile = repository.findByApplicationAndProfile("be-gateway-public", "public").get(0);
            profile.getProperties().addAll(properties);
            repository.save(profile);
            log.info("Mitre idp routes inserted. Modified: {}", properties);
        }
    }

    public void clearDefaultKeycloakCredentials(ConfigPropertiesRepository repository) {
        List<ConfigProfile> aDefault = repository.findByApplicationAndProfile("user-management", "default");
        if (aDefault.isEmpty()) {
            return;
        }
        Map<String, ConfigProperty> propertiesToDelete = new HashMap<>();
        propertiesToDelete.put("keycloak.admin.username", new ConfigProperty("keycloak.admin.username", "", false));
        propertiesToDelete.put("keycloak.admin.password", new ConfigProperty("keycloak.admin.password", "", false));
        propertiesToDelete.put("keycloak.admin.clientId", new ConfigProperty("keycloak.admin.clientId", "", false));

        Map<String, ConfigProperty> propertiesAsMap = aDefault.get(0).getPropertiesAsMap();
        propertiesAsMap.putAll(propertiesToDelete);
        aDefault.get(0).setPropertiesFromMap(propertiesAsMap);
        repository.save(aDefault.get(0));
    }

    public void changeKeycloakPropertiesName(ConfigPropertiesRepository repository) {
        Map<String, ConfigProperty> propertiesToDelete = new HashMap<>();
        propertiesToDelete.put("keycloak.authServerUrl", new ConfigProperty("keycloak.authServerUrl", "", false));
        propertiesToDelete.put("idp.authServerUrl", new ConfigProperty("idp.authServerUrl", "", false));
        propertiesToDelete.put("keycloak.authServersCount", new ConfigProperty("keycloak.authServersCount", "", false));
        propertiesToDelete.put("keycloak.sslRequiredType", new ConfigProperty("keycloak.sslRequiredType", "", false));
        propertiesToDelete.put("idp.authServersCount", new ConfigProperty("idp.authServersCount", "", false));
        propertiesToDelete.put("idp.sslRequiredType", new ConfigProperty("idp.sslRequiredType", "", false));
        propertiesToDelete.put("keycloak.clientId", new ConfigProperty("keycloak.clientId", "", false));
        propertiesToDelete.put("idp.clientId", new ConfigProperty("idp.clientId", "", false));
        propertiesToDelete.put("keycloak.gateway.clientId", new ConfigProperty("keycloak.gateway.clientId", "", false));
        propertiesToDelete.put("idp.gateway.clientId", new ConfigProperty("idp.gateway.clientId", "", false));
        propertiesToDelete.put("keycloak.gateway.clientSecret", new ConfigProperty("keycloak.gateway.clientSecret", "", false));
        propertiesToDelete.put("idp.gateway.clientSecret", new ConfigProperty("idp.gateway.clientSecret", "", false));
        propertiesToDelete.put("keycloak.gateway.route", new ConfigProperty("keycloak.gateway.route", "", false));
        propertiesToDelete.put("idp.gateway.route", new ConfigProperty("idp.gateway.route", "", false));

        ConfigProfile configProfile = repository.findByApplicationAndProfile("global", "default").get(0);
        Map<String, ConfigProperty> propertiesAsMap = configProfile.getPropertiesAsMap();
        propertiesAsMap.putAll(propertiesToDelete);
        configProfile.setPropertiesFromMap(propertiesAsMap);
        repository.save(configProfile);
    }

    public void fixEmailsAndCustomerSpecifier(ConfigPropertiesRepository repository) {
        Map<String, ConfigProperty> propertiesToDelete = new HashMap<>();
        propertiesToDelete.put("mail.cloudAdminEmail", new ConfigProperty("mail.cloudAdminEmail", "", false));
        propertiesToDelete.put("mail.fromEmail", new ConfigProperty("mail.fromEmail", "", false));
        propertiesToDelete.put("mail.server.host", new ConfigProperty("mail.server.host", "", false));
        propertiesToDelete.put("mail.server.user", new ConfigProperty("mail.server.user", "", false));
        propertiesToDelete.put("error.page.customerSpecifier", new ConfigProperty("error.page.customerSpecifier", "", false));

        ConfigProfile configProfile = repository.findByApplicationAndProfile("global", "default").get(0);
        Map<String, ConfigProperty> propertiesAsMap = configProfile.getPropertiesAsMap();
        propertiesAsMap.putAll(propertiesToDelete);
        configProfile.setPropertiesFromMap(propertiesAsMap);
        repository.save(configProfile);

        List<ConfigProperty> propertiesToAdd = new ArrayList<>(7);

        String emailUser = System.getenv("EMAIL_USER");
        String emailPassword = System.getenv("EMAIL_PASSWORD");
        boolean emailAuth = true;
        if (StringUtils.isBlank(emailUser)) {
            emailAuth = false;
            emailUser = "admin";
        }
        if (StringUtils.isBlank(emailPassword)) {
            emailAuth = false;
            emailPassword = "admin";
        }
        propertiesToAdd.add(new ConfigProperty("mail.server.auth", String.valueOf(emailAuth), false));
        propertiesToAdd.add(new ConfigProperty("mail.server.user", emailUser, false));
        propertiesToAdd.add(new ConfigProperty("mail.server.password", emailPassword, false));
        propertiesToAdd.add(new ConfigProperty("mail.server.host", System.getenv("EMAIL_HOST"), false));
        propertiesToAdd.add(new ConfigProperty("mail.cloudAdminEmail", System.getenv("CLOUD_ADMIN_EMAIL"), false));
        propertiesToAdd.add(new ConfigProperty("mail.fromEmail", System.getenv("EMAIL_FROM"), false));
        propertiesToAdd.add(new ConfigProperty("error.page.customerSpecifier", "default", false));

        configProfile.getProperties().addAll(propertiesToAdd);
        repository.save(configProfile);
    }

    private String getElasticSearchUrlOrDefault() {
        String elasticUrl = System.getenv("ELASTIC_URL");
        String elasticPort = System.getenv("ELASTIC_PORT");
        if (!StringUtils.isBlank(elasticUrl) && !StringUtils.isBlank(elasticPort)) {
            return elasticUrl + ":" + elasticPort;
        }
        return ELASTIC_URL_DEFAULT;
    }
}
