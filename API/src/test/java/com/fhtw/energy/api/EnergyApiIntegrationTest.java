package com.fhtw.energy.api;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
public class EnergyApiIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeAll
    static void setupContainer() {
        System.setProperty("spring.datasource.url", postgres.getJdbcUrl());
        System.setProperty("spring.datasource.username", postgres.getUsername());
        System.setProperty("spring.datasource.password", postgres.getPassword());
    }

    @Test
    void testGetCurrent_returnsDataFromRealDb() throws Exception {
        jdbcTemplate.execute("""
            CREATE TABLE current_percentage (
                hour TIMESTAMP,
                community_depleted DOUBLE PRECISION,
                grid_portion DOUBLE PRECISION
            );
        """);

        jdbcTemplate.execute("""
            INSERT INTO current_percentage (hour, community_depleted, grid_portion)
            VALUES (NOW(), 0.55, 0.45);
        """);

        mockMvc.perform(get("/energy/current"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.communityDepleted").value(0.55))
                .andExpect(jsonPath("$.gridPortion").value(0.45));
    }

    @Test
    void testGetHistorical_returnsCorrectSummary() throws Exception {
        jdbcTemplate.execute("""
        CREATE TABLE usage_hourly (
            hour TIMESTAMP,
            community_produced DOUBLE PRECISION,
            community_used DOUBLE PRECISION,
            grid_used DOUBLE PRECISION
        );
    """);

        // Daten für 1. Juni 2025 einfügen
        jdbcTemplate.execute("""
        INSERT INTO usage_hourly (hour, community_produced, community_used, grid_used) VALUES
        ('2025-06-01T08:00:00', 5.0, 3.0, 2.0),
        ('2025-06-01T12:00:00', 5.0, 4.5, 2.5);
    """);

        mockMvc.perform(get("/energy/historical")
                        .param("start", "2025-06-01T00:00:00")
                        .param("end", "2025-06-01T23:59:00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCommunityProduced").value(10.0))
                .andExpect(jsonPath("$.totalCommunityUsed").value(7.5))
                .andExpect(jsonPath("$.totalGridUsed").value(4.5));
    }

}
