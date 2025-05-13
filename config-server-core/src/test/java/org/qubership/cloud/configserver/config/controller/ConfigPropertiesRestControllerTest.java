package org.qubership.cloud.configserver.config.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.qubership.cloud.configserver.PostgresqlConfiguration;
import org.qubership.cloud.configserver.config.ApplicationWithProfiles;
import org.qubership.cloud.configserver.config.ConfigProfile;
import org.qubership.cloud.configserver.config.ConfigProperty;
import org.qubership.cloud.configserver.config.repository.ExtendedConfigPropertiesRepository;
import org.qubership.cloud.configserver.encryption.EncryptionService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.qubership.config.UnitTestApplicationConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.integration.support.locks.DefaultLockRegistry;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = {PostgresqlConfiguration.class, UnitTestApplicationConfig.class})
public class ConfigPropertiesRestControllerTest {

    private static final String APPLICATION_NAME = "some-cool-service";
    private static final String PROFILE = "default";
    private static final String KEY = "key";
    private static final String VALUE = "value";
    private static final Map<String, String> PROPERTIES = Collections.singletonMap(KEY, VALUE);

    @Autowired
    private ExtendedConfigPropertiesRepository configPropertiesRepository;
    @Autowired
    private EncryptionService encryptionService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper = new ObjectMapper();

    @Before
    public void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(new ConfigPropertiesController(encryptionService, configPropertiesRepository, new DefaultLockRegistry())).build();
    }

    @After
    public void tearDown() {
        configPropertiesRepository.deleteAll();
    }

    @Test
    public void testDeleteProfile() throws Exception {
        ConfigProfile configProfile = createConfigProfile();
        configPropertiesRepository.save(configProfile);

        mockMvc.perform(post("/" + APPLICATION_NAME + "/" + PROFILE + "/properties-delete")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());

        List<ConfigProfile> result =
                configPropertiesRepository.findByApplicationAndProfile(configProfile.getApplication(), configProfile.getProfile());

        assertTrue(result == null || result.isEmpty());
    }

    @Test
    public void testDeleteConcreteProperty() throws Exception {
        ConfigProfile configProfile = createConfigProfile();
        configPropertiesRepository.save(configProfile);

        mockMvc.perform(post("/" + APPLICATION_NAME + "/" + PROFILE + "/properties-delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(PROPERTIES.keySet())))
                .andDo(print())
                .andExpect(status().isOk());

        List<ConfigProfile> result =
                configPropertiesRepository.findByApplicationAndProfile(configProfile.getApplication(), configProfile.getProfile());

        assertNotNull(result);
        assertTrue(result.get(0).getProperties().isEmpty());
    }

    @Test
    public void testGetApplicationsWithProfiles() throws Exception {
        ConfigProfile configProfile1 = new ConfigProfile();
        ConfigProfile configProfile2 = new ConfigProfile();
        configProfile1.setApplication("application1");
        configProfile2.setApplication("application1");
        configProfile1.setProfile("default");
        configProfile2.setProfile("dev");
        configPropertiesRepository.save(configProfile1);
        configPropertiesRepository.save(configProfile2);

        ApplicationWithProfiles expectedApplicationWithProfiles = new ApplicationWithProfiles("application1", Arrays.asList("default", "dev"));

        MvcResult mvcResult = mockMvc.perform(get("/applications").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn();
        String configProfileInJson = mvcResult.getResponse().getContentAsString();

        assertEquals(objectMapper.writeValueAsString(Collections.singletonList(expectedApplicationWithProfiles)), configProfileInJson);
    }

    private ConfigProfile createConfigProfile() {
        ConfigProfile configProfile = new ConfigProfile();
        configProfile.setApplication(APPLICATION_NAME);
        configProfile.setProfile(PROFILE);

        ConfigProperty configProperty = new ConfigProperty();
        configProperty.setKey(KEY);
        configProperty.setValue(VALUE);

        configProfile.setPropertiesFromMap(Collections.singletonMap(KEY, configProperty));
        return configProfile;
    }
}
