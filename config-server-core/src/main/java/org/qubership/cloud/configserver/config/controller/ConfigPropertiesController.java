package org.qubership.cloud.configserver.config.controller;

import org.qubership.cloud.configserver.config.ApplicationWithProfiles;
import org.qubership.cloud.configserver.config.ConfigProfile;
import org.qubership.cloud.configserver.config.ConfigPropertiesValidator;
import org.qubership.cloud.configserver.config.ConfigProperty;
import org.qubership.cloud.configserver.config.repository.ConfigPropertiesRepository;
import org.qubership.cloud.configserver.config.repository.ExtendedConfigPropertiesRepository;
import org.qubership.cloud.configserver.encryption.EncryptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping(path = "/")
@Tag(name = "Config Properties", description = "Change and restore properties")
public class ConfigPropertiesController {
    private EncryptionService encryptor;
    private final LockRegistry lockRegistry;
    private final ConfigPropertiesValidator propertiesValidator = new ConfigPropertiesValidator();
    private static final String KEYS_ERROR_MESSAGE = "Property keys with errors: \n%s";
    private ExtendedConfigPropertiesRepository configPropertiesRepository;

    public ConfigPropertiesController(@Autowired(required = false) EncryptionService encryptor, @NonNull ExtendedConfigPropertiesRepository configPropertiesRepository, @NonNull LockRegistry lockRegistry) {
        this.encryptor = encryptor;
        this.configPropertiesRepository = configPropertiesRepository;
        this.lockRegistry = lockRegistry;
    }

    @Operation(summary = "Get applications list",
            description = "Returns list of configured applications with their profiles",operationId = "getApplicationsAndProfiles")
    @ApiResponse(responseCode = "200", content = @Content(schema = @Schema(implementation = ApplicationWithProfiles.class, type = "array")),description = "Returned successfully list of configured applications with their profiles")
    @RequestMapping(path = "/applications", method = {RequestMethod.GET}, produces = "application/json")
    public ResponseEntity<?> getApplicationsAndProfiles() {
        List<ApplicationWithProfiles> apps = configPropertiesRepository.findAll().stream().collect(Collectors.groupingBy(ConfigProfile::getApplication,
                        Collectors.mapping(ConfigProfile::getProfile, Collectors.toList())))
                .entrySet().stream().map(o -> new ApplicationWithProfiles(o.getKey(), o.getValue())).collect(Collectors.toList());
        return ResponseEntity.ok(apps);
    }

    public static List<ApplicationWithProfiles> getApplicationsWithProfilesList(ConfigPropertiesRepository configPropertiesRepository) {
        return configPropertiesRepository.findAll().stream().collect(Collectors.groupingBy(ConfigProfile::getApplication,
                        Collectors.mapping(ConfigProfile::getProfile, Collectors.toList())))
                .entrySet().stream().map(o -> new ApplicationWithProfiles(o.getKey(), o.getValue())).collect(Collectors.toList());
    }

    /**
     * Add new property for defined application and profile. If property is already existed than replace it with new
     * one. Create new Document with defined application, profile and properties in case of it is not exist yet.
     */
    @Operation(summary = "Add new property",
            description = "Adds new property for defined application and profile. " +
                    "If property is already existed than replace it with new one. " +
                    "Create new Document with defined application, profile and properties in case of it is not exist yet.",operationId = "addProperties")

    @ApiResponse(responseCode = "201", description = "Properties successfully added")
    @ApiResponse(responseCode = "504", description = "Aborted to write properties after timeout")
    @ApiResponse(responseCode = "400", content = @Content(examples = @ExampleObject(value = KEYS_ERROR_MESSAGE + " some-property")),description = "Bad Request")
    @ResponseStatus(HttpStatus.CREATED)
    @RequestMapping(path = "/{application}/{profile}",
            method = {RequestMethod.PUT, RequestMethod.POST}, consumes = "application/json")
    public ResponseEntity<?> addProperties(@PathVariable String application,
                                           @PathVariable String profile,
                                           @Parameter(description = "JSON mapping key string to value string", required = true)
                                           @RequestBody Map<String, String> properties) throws InterruptedException {
        log.info("Update properties for: {}/{}, properties: {}", application, profile, valueForLogger(properties));
        Lock lock = lockRegistry.obtain(lockKey(application, profile));
        if (!lock.tryLock(1, TimeUnit.MINUTES)) {
            log.error("Aborted write properties {} to application {}, profile {} after 1 minutes", valueForLogger(properties), application, profile);
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body("Aborted write properties " + properties + " to application " + application + ", profile " + profile + " after 1 minutes");
        }
        try {
            List<ConfigProfile> configProfiles = configPropertiesRepository.findByApplicationAndProfile(application, profile);

            Map<String, ConfigProperty> mapOfConfigProperties = new HashMap<>(properties.size());
            for (Map.Entry<String, String> entry : properties.entrySet()) {
                mapOfConfigProperties.put(entry.getKey(), new ConfigProperty(entry.getKey(), entry.getValue(), false));
            }

            // TODO: modification distributed synchronization
            if (configProfiles != null && !configProfiles.isEmpty()) {
                ConfigProfile currentProfile = configProfiles.get(0);
                Map<String, ConfigProperty> updatedProperties = new HashMap<>();
                encryptor.encryptUpdatedPropertiesIfNeeded(currentProfile.getPropertiesAsMap(), mapOfConfigProperties);
                updatedProperties.putAll(currentProfile.getPropertiesAsMap());
                updatedProperties.putAll(mapOfConfigProperties);
                ConfigPropertiesValidator.ValidationResult validationResult = propertiesValidator.validate(updatedProperties);
                if (!validationResult.isValid()) {
                    return buildErrorResponse(validationResult);
                }
                updateConfigProfile(currentProfile, updatedProperties);
            } else {
                ConfigPropertiesValidator.ValidationResult validationResult = propertiesValidator.validate(mapOfConfigProperties);
                if (!validationResult.isValid()) {
                    return buildErrorResponse(validationResult);
                }
                createConfigProfile(application, profile, mapOfConfigProperties);
            }
            return new ResponseEntity<>(HttpStatus.CREATED);
        } finally {
            lock.unlock();
        }
    }

    private Object lockKey(String application, String profile) {
        return application + ":" + profile;
    }

    /**
     * Delete properties for defined application and profile.
     */
    @Operation(summary = "Delete properties",
            description = "Deletes properties for defined application and profile. " +
                    "If properties was not specified would delete all of them.",operationId = "deleteProperties")
    @ApiResponse(responseCode = "200", description = "Properties successfully deleted")
    @ApiResponse(responseCode = "504", description = "Aborted to write properties after timeout")
    @ResponseStatus(HttpStatus.OK)
    @RequestMapping(path = "/{application}/{profile}/properties-delete",
            method = {RequestMethod.POST}, consumes = "application/json")
    public ResponseEntity<?> deleteProperties(@PathVariable String application,
                                              @PathVariable String profile,
                                              @Parameter(description = "JSON list with names of properties")
                                              @RequestBody(required = false) List<String> properties) throws InterruptedException {
        final String propertiesStr = properties != null ? properties.toString() : "all";
        log.info("Delete properties for: {}/{}, properties: {}", application, profile, propertiesStr);
        Lock lock = lockRegistry.obtain(lockKey(application, profile));
        if (!lock.tryLock(1, TimeUnit.MINUTES)) {
            log.error("Aborted delete properties {} to application {}, profile {} after 1 minutes", propertiesStr, application, profile);
            return ResponseEntity.status(HttpStatus.GATEWAY_TIMEOUT).body("Aborted delete properties "
                    + propertiesStr + " to application " + application + ", profile " + profile + " after 1 minutes");
        }
        try {
            List<ConfigProfile> configProfiles = configPropertiesRepository.findByApplicationAndProfile(application, profile);

            // TODO: modification distributed synchronization
            if (properties != null && configProfiles != null && !configProfiles.isEmpty()) {
                ConfigProfile currentProfile = configProfiles.get(0);
                configPropertiesRepository.deleteProperties(currentProfile, properties);
            }

            if (properties == null && configProfiles != null) {
                configPropertiesRepository.deleteAll(configProfiles);
            }
            return new ResponseEntity<>(HttpStatus.OK);
        } finally {
            lock.unlock();
        }
    }

    private void updateConfigProfile(ConfigProfile currentProfile, Map<String, ConfigProperty> properties) {
        log.debug("Update config: {}", currentProfile);

        currentProfile.setVersion(currentProfile.getVersion() + 1);
        currentProfile.setPropertiesFromMap(properties);
        configPropertiesRepository.save(currentProfile);
    }

    private void createConfigProfile(String application, String profile, Map<String, ConfigProperty> properties) {
        ConfigProfile newConfigProfile = new ConfigProfile();
        newConfigProfile.setApplication(application);
        newConfigProfile.setProfile(profile);
        newConfigProfile.setPropertiesFromMap(properties);
        log.debug("Create config: {}", newConfigProfile);
        configPropertiesRepository.save(newConfigProfile);
    }

    private ResponseEntity<String> buildErrorResponse(ConfigPropertiesValidator.ValidationResult validationResult) {
        StringBuilder properties = new StringBuilder();
        for (String property : validationResult.getConflictingProperties()) {
            properties.append(property).append("\n");
        }
        for (String property : validationResult.getDotProperties()) {
            properties.append(property).append("\n");
        }
        String errorMessage = String.format(KEYS_ERROR_MESSAGE, properties);
        log.error(errorMessage);
        return ResponseEntity.badRequest().body(errorMessage);
    }

    private Map<String, String> valueForLogger(Map<String, String> properties) {
        return properties.entrySet().stream()
                .filter(property -> !property.getKey().toLowerCase(Locale.ROOT).contains("password"))
                .collect(Collectors.toMap(x -> x.getKey(), x -> x.getValue()));
    }

}
