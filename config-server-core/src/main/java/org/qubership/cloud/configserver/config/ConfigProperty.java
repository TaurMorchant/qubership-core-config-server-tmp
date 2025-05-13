package org.qubership.cloud.configserver.config;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import java.util.UUID;

@Entity(name = "config_property")
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConfigProperty {
    @Id
    @GeneratedValue
    private UUID id;
    @Column(name = "\"key\"")
    private String key;
    @Column(name = "\"value\"")
    private String value;
    private Boolean encrypted;

    public ConfigProperty(String key, String value, Boolean encrypted) {
        this.key = key;
        this.value = value;
        this.encrypted = encrypted;
    }

    public Boolean isEncrypted() {
        if (this.encrypted == null) {
            return false;
        }
        return this.encrypted;
    }
}
