package at.fhtw.currentpercentageservice;

import java.sql.*;
import java.time.LocalDateTime;

public class PercentageCalculator {

    private static String dbUrl;
    private static String dbUser;
    private static String dbPass;

    public PercentageCalculator(String dbUrl, String dbUser, String dbPass) {
        PercentageCalculator.dbUrl = dbUrl;
        PercentageCalculator.dbUser = dbUser;
        PercentageCalculator.dbPass = dbPass;
    }

    /**
     * Receives new Data from the Server
     * Receives Community produced, Community Used and Grid Used
     * Calculates Community Depleted and Grid Portion
     * sends a new Query with Community Depleted and Grid Portion
     * @param hour the current hour
     */
    public void handleUpdate(LocalDateTime hour) {
        try (Connection db = DriverManager.getConnection(dbUrl, dbUser, dbPass)) {
            PreparedStatement query = db.prepareStatement(
                    "SELECT community_produced, community_used, grid_used FROM usage_hourly WHERE hour = ?"
            );
            query.setTimestamp(1, Timestamp.valueOf(hour));
            ResultSet rs = query.executeQuery();

            if (rs.next()) {
                double communityProduced = rs.getDouble("community_produced");
                double communityUsed = rs.getDouble("community_used");
                double gridUsed = rs.getDouble("grid_used");

                double communityDepleted = (communityProduced > 0)
                        ? Math.min(100, (communityUsed / communityProduced) * 100)
                        : 0;
                double gridPortion = (communityUsed > 0) ? (gridUsed / communityUsed) * 100 : 0;

                PreparedStatement update = db.prepareStatement(
                        "INSERT INTO current_percentage (hour, community_depleted, grid_portion) " +
                                "VALUES (?, ?, ?) " +
                                "ON CONFLICT (hour) DO UPDATE SET " +
                                "community_depleted = EXCLUDED.community_depleted, " +
                                "grid_portion = EXCLUDED.grid_portion"
                );
                update.setTimestamp(1, Timestamp.valueOf(hour));
                update.setDouble(2, communityDepleted);
                update.setDouble(3, gridPortion);
                update.executeUpdate();

                System.out.printf("[✓] Calculated percentage for %s → Grid: %.2f%%%n", hour, gridPortion);
            } else {
                System.out.println("[!] No usage data found for hour: " + hour);
            }

        } catch (SQLException e) {
            System.err.println("[!] Database error:");
            e.printStackTrace();
        }
    }

}
