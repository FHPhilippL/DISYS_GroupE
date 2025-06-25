package com.fhtw.energy.api.service;

import com.fhtw.energy.api.model.CurrentPercentage;
import com.fhtw.energy.api.model.HourlyUsage;
import com.fhtw.energy.api.model.UsageSummary;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service class responsible for retrieving energy data from the database.
 *
 * This class provides methods to:
 * 1. Fetch the most recent current usage percentage from the database.
 * 2. Fetch summarized energy usage (totals) for a given time range.
 * 3. Fetch detailed hourly usage records within a time range.
 *
 * All methods use JdbcTemplate to query the database and are safe for use
 * in REST controllers. Logging is included for tracking incoming requests
 * and troubleshooting failures.
 */
@Service
public class EnergyService {

    private static final Logger logger = LoggerFactory.getLogger(EnergyService.class);

    private final JdbcTemplate jdbcTemplate;

    public EnergyService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Retrieves the most recent energy usage percentage data from the database.
     * The data includes community depletion and grid usage from the latest hour.
     *
     * @return the most recent CurrentPercentage object, or null if none found
     */
    public CurrentPercentage getCurrentPercentage() {
        logger.info("Fetching current percentage data from database");
        String sql = "SELECT * FROM current_percentage ORDER BY hour DESC LIMIT 1";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            CurrentPercentage cp = new CurrentPercentage();
            cp.setHour(rs.getTimestamp("hour").toLocalDateTime());
            cp.setCommunityDepleted(rs.getDouble("community_depleted"));
            cp.setGridPortion(rs.getDouble("grid_portion"));
            return cp;
        }).stream().findFirst().orElse(null);
    }

    /**
     * Retrieves total energy usage and production data for the specified time range.
     *
     * @param start the start of the time range (inclusive)
     * @param end the end of the time range (inclusive)
     * @return UsageSummary object containing totals for the given period
     */
    public UsageSummary getHistorical(LocalDateTime start, LocalDateTime end) {
        logger.info("Fetching historical usage summary from {} to {}", start, end);
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

    /**
     * Retrieves detailed hourly energy usage data for the specified time range.
     *
     * @param start the start of the time range (inclusive)
     * @param end the end of the time range (inclusive)
     * @return List of HourlyUsage records, one per hour in the specified range
     */
    public List<HourlyUsage> getHistoricalDetailed(LocalDateTime start, LocalDateTime end) {
        logger.info("Fetching detailed hourly usage from {} to {}", start, end);
        String sql = """
            SELECT hour, community_produced, community_used
            FROM usage_hourly
            WHERE hour BETWEEN ? AND ?
            ORDER BY hour
        """;

        return jdbcTemplate.query(sql, (rs, rowNum) -> new HourlyUsage(
                rs.getTimestamp("hour").toLocalDateTime(),
                rs.getDouble("community_produced"),
                rs.getDouble("community_used")
        ), start, end);
    }
}
