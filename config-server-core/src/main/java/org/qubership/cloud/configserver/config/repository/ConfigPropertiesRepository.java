package org.qubership.cloud.configserver.config.repository;

import org.qubership.cloud.configserver.config.ConfigProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ConfigPropertiesRepository extends JpaRepository<ConfigProfile, UUID> {
    List<ConfigProfile> findByApplicationAndProfile(String serviceName, String profile);
}
