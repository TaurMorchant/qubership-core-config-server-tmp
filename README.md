# Config Server

This project consists of two modules: `config-server-app` and `config-server-core`. The `config-server-app` module is a Spring Boot application that uses the `config-server-core` module to provide configuration management functionality.

## Project Structure

### config-server-app

- **Source Directory**: `src/main/java/org/qubership/cloud/configserver`
- **Main Application Class**: `Application.java`
- **Dependencies**:
  - `config-server-core`
  - `flyway-core`
  - Spring Boot Test
  - Testcontainers

### config-server-core

- **Source Directory**: `src/main/java/org/qubership/cloud/configserver`
- **Key Components**:
  - **Monitoring**: Contains classes like `PostgreSqlDbHealthCheck` and `HealthCheckStatus`.
  - **Encryption**: Contains `EncryptionService` and related configuration classes.
  - **Config**: Contains controllers, services, repositories, and POJOs for managing configuration properties.
- **Dependencies**:
  - Spring Boot Actuator
  - Spring Boot Integration
  - Spring Cloud Context
  - Micrometer Observation
  - Spring Boot Web
  - Spring Boot Data JPA
  - PostgreSQL JDBC
  - Lombok
  - Spring Security
  - Flyway Core
  - Micrometer Registry Prometheus
  - Consul Client
  - Various Jakarta and JAXB APIs
  - Testing dependencies (Mockito, Testcontainers, etc.)

## Configuration

The `config-server-core` module uses the following configuration files:

- **application.yml**: Contains server, encryption, database, Consul, management, and Spring Cloud configurations.
- **logback.xml**: Logging configuration.
- **policies.conf**: Policy configurations.
- **bootstrap.yml**: Bootstrap configuration.
- **application-tls.properties**: TLS configuration.

## Usage

To run the `config-server-app`, ensure that the necessary environment variables are set, and then execute the main application class `Application.java`.

## Additional Information

For more details, refer to the source code and configuration files in the respective modules.
