package com.fhtw.energy.api.controller;

import com.fhtw.energy.api.model.CurrentPercentage;
import com.fhtw.energy.api.model.UsageSummary;
import com.fhtw.energy.api.service.EnergyService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ContextConfiguration(classes = {EnergyController.class, EnergyControllerTest.TestConfig.class})
@WebMvcTest
@Import(EnergyControllerTest.TestConfig.class)
class EnergyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EnergyService energyService;

    @Configuration
    static class TestConfig {
        @Bean
        public EnergyService energyService() {
            return Mockito.mock(EnergyService.class);
        }
    }

    @Test
    @DisplayName("GET /energy/current returns current percentage")
    void testGetCurrentPercentage() throws Exception {
        CurrentPercentage mock = new CurrentPercentage();
        mock.setCommunityDepleted(0.65);
        mock.setGridPortion(0.35);
        mock.setHour(LocalDateTime.now());

        when(energyService.getCurrentPercentage()).thenReturn(mock);

        mockMvc.perform(get("/energy/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.communityDepleted").value(0.65))
                .andExpect(jsonPath("$.gridPortion").value(0.35));
    }

    @Test
    @DisplayName("GET /energy/historical returns usage summary")
    void testGetHistorical() throws Exception {
        UsageSummary mockSummary = new UsageSummary(5.0, 3.0, 2.0);
        LocalDateTime start = LocalDateTime.of(2025, 6, 1, 0, 0);
        LocalDateTime end = LocalDateTime.of(2025, 6, 1, 23, 59);

        when(energyService.getHistorical(start, end)).thenReturn(mockSummary);

        mockMvc.perform(get("/energy/historical")
                        .param("start", start.toString())
                        .param("end", end.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCommunityProduced").value(5.0))
                .andExpect(jsonPath("$.totalCommunityUsed").value(3.0))
                .andExpect(jsonPath("$.totalGridUsed").value(2.0));
    }

    @Test
    @DisplayName("GET /energy/historical without parameters returns 400")
    void testHistoricalMissingParams() throws Exception {
        mockMvc.perform(get("/energy/historical"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /energy/historical with invalid date returns 400")
    void testHistoricalInvalidDate() throws Exception {
        mockMvc.perform(get("/energy/historical")
                        .param("start", "INVALID_DATE")
                        .param("end", "2025-06-01T23:59"))
                .andExpect(status().isBadRequest());
    }

}
