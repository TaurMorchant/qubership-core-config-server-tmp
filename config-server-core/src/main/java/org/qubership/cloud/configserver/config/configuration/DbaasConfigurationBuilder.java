package org.qubership.cloud.configserver.config.configuration;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

import static java.util.Objects.isNull;

@Component
public class DbaasConfigurationBuilder {
    public static final String DB_CLASSIFIER = "default";
    private final String localDevelopmentNamespace;
    private final String microServiceName;

    public DbaasConfigurationBuilder(Environment environment) {
        this.microServiceName = environment.getProperty("spring.application.name");
        this.localDevelopmentNamespace = calculateLocalDevelopmentNamespaceValue(environment);
    }

    public Map<String, Object> createClassifier(String tenantId) {
        Map<String, Object> mapClassifier = new HashMap<>();
        mapClassifier.put("microserviceName", microServiceName);
        if (!isNull(tenantId)) {
            mapClassifier.put("tenantId", tenantId);
            mapClassifier.put("scope", "tenant");
        } else {
            mapClassifier.put("scope", "service");
        }
        mapClassifier.put("dbClassifier", DB_CLASSIFIER);
        if (localDevelopmentNamespace != null) {
            mapClassifier.put("localdev", localDevelopmentNamespace);
        }
        return mapClassifier;
    }

    private String calculateLocalDevelopmentNamespaceValue(Environment environment) {
        boolean attachToCloudDbFlag = "true".equalsIgnoreCase(environment.getProperty("attachToCloudDB"));
        return attachToCloudDbFlag ? null : environment.getProperty("LOCALDEV_NAMESPACE");
    }
}
