package org.qubership.cloud.configserver.config;

import lombok.Data;
import lombok.NonNull;

import java.util.List;

@Data
public class ApplicationWithProfiles {
    @NonNull
    private String name;
    @NonNull
    private List<String> profiles;
}
