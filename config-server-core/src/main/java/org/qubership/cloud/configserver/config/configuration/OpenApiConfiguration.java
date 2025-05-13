package org.qubership.cloud.configserver.config.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfiguration {

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("config-server API")
                        .version("2.0.0-SNAPSHOT")
                        .description("This is the API documentation for the config-server. With the Config Server you have a central place to manage external properties for applications across all environments."));
    }
}
