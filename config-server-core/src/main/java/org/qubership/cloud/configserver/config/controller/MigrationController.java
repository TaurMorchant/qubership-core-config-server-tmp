package org.qubership.cloud.configserver.config.controller;

import org.qubership.cloud.configserver.config.service.ConsulMigrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(path = "/api/v1")
public class MigrationController {

    private final ConsulMigrationService consulMigrationService;

    public MigrationController(@Autowired(required = false) ConsulMigrationService consulMigrationService) {
        this.consulMigrationService = consulMigrationService;
    }

    @Operation(summary = "Migrate to Consul",
            description = "Migrates all data from Postgresql to Consul",operationId = "migrateToConsul")
    @ApiResponse(responseCode = "200",description = "Migrated all data from Postgresql to Consul")
    @PostMapping(path = "/migrate", produces = "text/plain")
    public ResponseEntity<String> migrateToConsul() {
        if (consulMigrationService == null) {
            return ResponseEntity.badRequest().body("Consul is not available. Please set valid Consul properties.");
        }
        consulMigrationService.migrateToConsul();
        return ResponseEntity.ok().body("migrated");
    }
}
