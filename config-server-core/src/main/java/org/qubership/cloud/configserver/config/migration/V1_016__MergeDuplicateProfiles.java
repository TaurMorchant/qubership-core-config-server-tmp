package org.qubership.cloud.configserver.config.migration;

import org.qubership.cloud.configserver.config.ApplicationWithProfiles;
import org.qubership.cloud.configserver.config.ConfigProfile;
import org.qubership.cloud.configserver.config.ConfigProperty;
import org.qubership.cloud.configserver.config.SpringUtility;
import org.qubership.cloud.configserver.config.controller.ConfigPropertiesController;
import org.qubership.cloud.configserver.config.repository.ConfigPropertiesRepository;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@NoArgsConstructor
public class V1_016__MergeDuplicateProfiles extends BaseJavaMigration {

    private ConfigPropertiesRepository repository = SpringUtility.getBean(ConfigPropertiesRepository.class);
    private Map<String, ConfigProperty> mergedProperties = new HashMap<>();
    private ConfigProfile configProfileForMerge;

    @Override
    public void migrate(Context context) throws Exception {
        List<ApplicationWithProfiles> apps = ConfigPropertiesController.getApplicationsWithProfilesList(repository);

        for (ApplicationWithProfiles application : apps) {
            for (String appProfile : application.getProfiles()) {
                String appName = application.getName();
                List<ConfigProfile> profiles =
                        repository.findByApplicationAndProfile(appName, appProfile);
                boolean multipleProfiles = profiles.size() > 1;

                if (multipleProfiles) {
                    log.info("For application {} profile {} amount is: {}",
                            appName, appProfile, profiles.size());
                    profiles.stream().forEach(configProfile -> {
                        mergedProperties.putAll(configProfile.getPropertiesAsMap());
                    });

                    repository.deleteAll(profiles);

                    ConfigProfile.ConfigProfileBuilder cpb = ConfigProfile.builder()
                            .application(appName).profile(appProfile).version(1);
                    for (ConfigProperty property : mergedProperties.values()) {
                        cpb.property(
                                new ConfigProperty(property.getKey(), property.getValue(), property.getEncrypted()));
                    }
                    configProfileForMerge = cpb.build();
                    repository.save(configProfileForMerge);
                    log.info("Merged profile is saved.");
                }
            }
        }
    }
}
