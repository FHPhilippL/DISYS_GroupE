package com.fhtw.energy.api.service;

import com.fhtw.energy.api.model.CurrentPercentage;
import com.fhtw.energy.api.model.UsageSummary;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

class EnergyServiceTest {

    JdbcTemplate jdbcTemplate = Mockito.mock(JdbcTemplate.class);
    EnergyService service = new EnergyService(jdbcTemplate);

    @Test
    void testGetCurrentPercentage() throws Exception {
        CurrentPercentage cp = new CurrentPercentage();
        cp.setHour(LocalDateTime.of(2025, 6, 1, 12, 0));
        cp.setCommunityDepleted(0.6);
        cp.setGridPortion(0.4);

        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(List.of(cp));

        CurrentPercentage result = service.getCurrentPercentage();

        assertThat(result).isNotNull();
        assertThat(result.getCommunityDepleted()).isEqualTo(0.6);
        assertThat(result.getGridPortion()).isEqualTo(0.4);
    }

    @Test
    void testGetHistorical() throws Exception {
        UsageSummary expected = new UsageSummary(10.0, 7.5, 2.5);

        when(jdbcTemplate.queryForObject(
                anyString(),
                any(RowMapper.class),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(expected);

        UsageSummary result = service.getHistorical(
                LocalDateTime.of(2025, 6, 1, 0, 0),
                LocalDateTime.of(2025, 6, 1, 23, 59)
        );

        assertThat(result).isNotNull();
        assertThat(result.getTotalCommunityProduced()).isEqualTo(10.0);
        assertThat(result.getTotalCommunityUsed()).isEqualTo(7.5);
        assertThat(result.getTotalGridUsed()).isEqualTo(2.5);
    }
}
