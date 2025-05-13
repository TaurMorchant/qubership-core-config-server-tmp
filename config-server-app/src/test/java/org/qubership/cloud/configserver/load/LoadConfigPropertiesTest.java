package org.qubership.cloud.configserver.load;

import org.junit.jupiter.api.Disabled;
import org.qubership.cloud.configserver.Application;
import org.qubership.cloud.configserver.config.configuration.PostgresqlConfiguration;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.Repeat;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

//todo vlla test temporary disabled - need to analyze reason of failure
@Disabled
@WebAppConfiguration
@DirtiesContext
@SpringBootTest(
        classes = {
                Application.class,
                PostgresqlConfiguration.class
        }
)
public class LoadConfigPropertiesTest {
    private MockMvc mockMvc;
    @Autowired
    private WebApplicationContext context;
    @Autowired
    private ConfigurableEnvironment environment;

    @BeforeEach
    public void setup() {
        MutablePropertySources propertySources = environment.getPropertySources();
        Map<String, Object> properties = new HashMap<>();
        propertySources.addFirst(new MapPropertySource("custom properties", properties));
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .build();
    }

    @Test
    @Repeat(value = 10)
    public void test50Properties10Times() throws Exception {
        mockMvc.perform(post("/app/prof")
                        .content("{\"initial\":\"value\"}")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated());

        Map<String, String> props = new ConcurrentHashMap<>();
        props.put("initial", "value");
        int propNum = 50;
        int totalPropNum = propNum + 1;
        ExecutorService threads = Executors.newFixedThreadPool(propNum);

        threads.invokeAll(Stream.generate(() -> (Callable<Object>) () -> {
                            long threadId = Thread.currentThread().getId();
                            String key = "key" + threadId;
                            String value = "value" + threadId;
                            props.put(key, value);
                            mockMvc.perform(post("/app/prof")
                                            .content("{\"" + key + "\":\"" + value + "\"}")
                                            .contentType(MediaType.APPLICATION_JSON))
                                    .andExpect(status().isCreated());
                            return null;
                        }).limit(propNum)
                        .collect(Collectors.toList())
        ).forEach((future) -> {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        });

        threads.shutdown();
        Assertions.assertEquals(totalPropNum, props.size());
    }
}
