package com.fhtw.energy.api.service;

import com.fhtw.energy.api.model.CurrentPercentage;

import com.fhtw.energy.api.model.UsageSummary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class EnergyService {

    private final JdbcTemplate jdbcTemplate;

    public EnergyService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public CurrentPercentage getCurrentPercentage() {
        String sql = "SELECT * FROM current_percentage ORDER BY hour DESC LIMIT 1";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            CurrentPercentage cp = new CurrentPercentage();
            cp.setHour(rs.getTimestamp("hour").toLocalDateTime());
            cp.setCommunityDepleted(rs.getDouble("community_depleted"));
            cp.setGridPortion(rs.getDouble("grid_portion"));
            return cp;
        }).stream().findFirst().orElse(null);
    }

    public UsageSummary getHistorical(LocalDateTime start, LocalDateTime end) {
        String sql = """
                SELECT 
                    COALESCE(SUM(community_produced), 0), 
                    COALESCE(SUM(community_used), 0), 
                    COALESCE(SUM(grid_used), 0)
                FROM usage_hourly
                WHERE hour BETWEEN ? AND ?
            """;

        return jdbcTemplate.queryForObject(sql, (rs, rowNum) ->
                new UsageSummary(
                        rs.getDouble(1),
                        rs.getDouble(2),
                        rs.getDouble(3)
                ), start, end);
    }

}
