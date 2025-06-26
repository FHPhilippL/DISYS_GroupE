package at.fhtw.usageservice.infrastructure;

import at.fhtw.usageservice.interfaces.UsageRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import javax.sql.DataSource;

/**
 * JDBC-based implementation of the {@link UsageRepository} interface.
 * This class is responsible for persisting hourly community energy usage data
 * into a PostgreSQL database using a SQL UPSERT pattern.
 */
public class JdbcUsageRepository implements UsageRepository {

    private static final Logger logger = LoggerFactory.getLogger(JdbcUsageRepository.class);
    private final DataSource dataSource;

    /**
     * Constructs the repository with the given data source.
     *
     * @param dataSource a JDBC DataSource providing connections to the database
     */
    public JdbcUsageRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Saves or updates hourly energy usage data into the database.
     * If a record for the specified hour already exists, it is updated.
     * Otherwise, a new record is inserted.
     *
     * @param hour     the hour to which this usage data belongs
     * @param produced the total energy produced in that hour
     * @param used     the total community usage in that hour
     * @param grid     the energy consumed from the grid in that hour
     */
    @Override
    public void saveHourlyUsage(LocalDateTime hour, double produced, double used, double grid) {
        String sql = """
            INSERT INTO usage_hourly (hour, community_produced, community_used, grid_used)
            VALUES (?, ?, ?, ?)
            ON CONFLICT (hour) DO UPDATE SET
                community_produced = EXCLUDED.community_produced,
                community_used = EXCLUDED.community_used,
                grid_used = EXCLUDED.grid_used
        """;

        // Attempt to obtain a connection and execute the UPSERT statement
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(hour));
            stmt.setDouble(2, produced);
            stmt.setDouble(3, used);
            stmt.setDouble(4, grid);

            stmt.executeUpdate();
        } catch (SQLException e) {
            // Log any database access issues
            logger.error("Failed to save hourly usage to database", e);
        }
    }
}
