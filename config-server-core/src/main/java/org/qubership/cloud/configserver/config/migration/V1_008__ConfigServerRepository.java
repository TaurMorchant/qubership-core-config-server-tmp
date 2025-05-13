package org.qubership.cloud.configserver.config.migration;

import org.qubership.cloud.configserver.config.ConfigProfile;
import org.qubership.cloud.configserver.config.ConfigProperty;
import org.qubership.cloud.configserver.config.SpringUtility;
import org.qubership.cloud.configserver.config.repository.ConfigPropertiesRepository;
import lombok.NoArgsConstructor;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

@NoArgsConstructor
public class V1_008__ConfigServerRepository extends BaseJavaMigration {

    private ConfigPropertiesRepository repository = SpringUtility.getBean(ConfigPropertiesRepository.class);

    @Override
    public void migrate(Context context) throws Exception {
        ConfigProfile tenantActivatorDefaultProfile = ConfigProfile.builder()
                .application("dmp-tenant-activator")
                .profile("default")
                .property(new ConfigProperty("tenant.shoppingFrontend.templateName", "qubership-cloud-shopping-frontend", false))
                .build();
        repository.save(tenantActivatorDefaultProfile);

    }
}
