package at.fhtw.currentpercentageservice.testInfrastructure;

import at.fhtw.currentpercentageservice.testInterfaces.UsageRepository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class JdbcUsageRepository implements UsageRepository {

    private final DataSource dataSource;

    public JdbcUsageRepository(DataSource dataSource) {
        this.dataSource = dataSource;
    }

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

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setTimestamp(1, Timestamp.valueOf(hour));
            stmt.setDouble(2, produced);
            stmt.setDouble(3, used);
            stmt.setDouble(4, grid);

            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[!] Database error:");
            e.printStackTrace();
        }
    }
}
