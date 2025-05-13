package org.qubership.cloud.configserver.config.controller;

import org.qubership.cloud.configserver.config.service.ConsulMigrationService;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
public class MigrationControllerTest {

    @Test
    public void migrateToConsul() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new MigrationController(null)).build();
        MvcResult result = mockMvc.perform(post("/api/v1/migrate"))
                .andExpect(status().isBadRequest())
                .andReturn();
        assertEquals("Consul is not available. Please set valid Consul properties.", result.getResponse().getContentAsString());

        ConsulMigrationService consulMigrationService = mock(ConsulMigrationService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new MigrationController(consulMigrationService)).build();
        result = mockMvc.perform(post("/api/v1/migrate"))
                .andExpect(status().isOk())
                .andReturn();
        verify(consulMigrationService).migrateToConsul();
        assertEquals("migrated", result.getResponse().getContentAsString());
    }
}