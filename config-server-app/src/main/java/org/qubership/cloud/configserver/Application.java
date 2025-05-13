package org.qubership.cloud.configserver;

import org.qubership.cloud.log.manager.spring.LoggingFilter;
import org.qubership.cloud.microserviceframework.BaseApplicationOnRestTemplate;
import org.qubership.cloud.microserviceframework.application.MicroserviceApplicationBuilder;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@EnableConfigServer
@SpringBootApplication(excludeName = {
        "org.springframework.boot.autoconfigure.internalCachingMetadataReaderFactory",
        "org.springframework.boot.autoconfigure.websocket.servlet.WebSocketServletAutoConfiguration",
        "server-org.springframework.boot.autoconfigure.web.ServerProperties",
        "spring.http-org.springframework.boot.autoconfigure.http.HttpProperties",
        "org.springframework.boot.autoconfigure.task.TaskExecutionAutoConfiguration",
        "spring.task.execution-org.springframework.boot.autoconfigure.task.TaskExecutionProperties",
        "org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration",
        "spring.resources-org.springframework.boot.autoconfigure.web.ResourceProperties",
        "org.springframework.boot.autoconfigure.transaction.jta.JtaAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.web.servlet.ServletManagementContextAutoConfiguration",
        "management.health.status-org.springframework.boot.actuate.autoconfigure.health.HealthIndicatorProperties",
        "org.springframework.boot.autoconfigure.info.ProjectInfoAutoConfiguration",
        "spring.info-org.springframework.boot.autoconfigure.info.ProjectInfoProperties",
        "org.springframework.boot.actuate.autoconfigure.info.InfoContributorAutoConfiguration",
        "management.info-org.springframework.boot.actuate.autoconfigure.info.InfoContributorProperties",
        "org.springframework.cloud.autoconfigure.LifecycleMvcEndpointAutoConfiguration",
        "org.springframework.boot.actuate.autoconfigure.system.DiskSpaceHealthContributorAutoConfiguration",
        "management.health.diskspace-org.springframework.boot.actuate.autoconfigure.system.DiskSpaceHealthIndicatorProperties",
        "org.springframework.boot.actuate.autoconfigure.health.HealthContributorAutoConfiguration",
        "org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration",
        "spring.gson-org.springframework.boot.autoconfigure.gson.GsonProperties",
        "org.springframework.boot.autoconfigure.aop.AopAutoConfiguration",
        "org.springframework.boot.autoconfigure.dao.PersistenceExceptionTranslationAutoConfiguration",
        "org.springframework.boot.autoconfigure.data.web.SpringDataWebAutoConfiguration",
        "spring.data.web-org.springframework.boot.autoconfigure.data.web.SpringDataWebProperties",
        "org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration",
        "org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration",
        "org.springframework.boot.autoconfigure.task.TaskSchedulingAutoConfiguration",
        "spring.task.scheduling-org.springframework.boot.autoconfigure.task.TaskSchedulingProperties",
        "org.springframework.boot.autoconfigure.transaction.TransactionAutoConfiguration",
        "spring.transaction-org.springframework.boot.autoconfigure.transaction.TransactionProperties",
        "org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration",
        "spring.servlet.multipart-org.springframework.boot.autoconfigure.web.servlet.MultipartProperties",
        "management.server-org.springframework.boot.actuate.autoconfigure.web.server.ManagementServerProperties"
})
public class Application extends BaseApplicationOnRestTemplate {
    public static void main(String[] args) {
        if(System.getProperty("core.contextpropagation.providers.xversion.sync.enabled") == null){
            System.setProperty("core.contextpropagation.providers.xversion.sync.enabled", "false");
        }
        new MicroserviceApplicationBuilder()
                .withApplicationClass(Application.class)
                .withExceptFilterClasses(LoggingFilter.class)
                .withArguments(args)
                .build();
    }
}
