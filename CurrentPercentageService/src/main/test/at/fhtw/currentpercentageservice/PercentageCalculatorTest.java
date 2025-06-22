package at.fhtw.currentpercentageservice;

import at.fhtw.currentpercentageservice.testApplication.UsageMessageHandler;
import at.fhtw.currentpercentageservice.testInfrastructure.JdbcUsageRepository;
import at.fhtw.currentpercentageservice.testInfrastructure.RabbitUsageMessenger;
import at.fhtw.currentpercentageservice.testInterfaces.UsageMessenger;
import at.fhtw.currentpercentageservice.testInterfaces.UsageRepository;
import com.google.gson.Gson;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PercentageCalculatorTest extends IntegrationTestBase{

    private UsageMessageHandler handler; // Handler um Testdaten in die Datenbank zu schicken
    private final Gson gson = new Gson(); // Für JSON-Verarbeitung der Nachrichten
    private PercentageCalculator calculator;

    /**
     * Wird vor jedem einzelnen Test aufgerufen.
     * Baut den Handler mit echter RabbitMQ- und Datenbankanbindung.
     */
    @BeforeEach
    void Testsetup() {
        // Repository mit echter (Testcontainer-)Datenbank
        UsageRepository repo = new JdbcUsageRepository(dataSource);

        // Messenger für RabbitMQ mit Queue "energy.updated"
        UsageMessenger messenger = new RabbitUsageMessenger(channel, "energy.updated");

        // Handler zusammenbauen
        handler = new UsageMessageHandler(repo, messenger);

        calculator = new PercentageCalculator(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    @Test
    void ensureCommunityUsedPercentageIsCalculatedCorrectly() throws Exception {
        //Arrange
        LocalDateTime timestamp = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        String hourKey = timestamp.toString();

        // Simulierte PRODUCER-Nachricht
        Map<String, Object> production = Map.of(
                "type", "PRODUCER",
                "association", "COMMUNITY",
                "kwh", 10.0,
                "datetime", hourKey
        );

        // Simulierte USER-Nachricht
        Map<String, Object> usage = Map.of(
                "type", "USER",
                "association", "COMMUNITY",
                "kwh", 7.5,
                "datetime", hourKey
        );

        //Act
        handler.handleMessage(production);
        handler.handleMessage(usage);

        calculator.handleUpdate(timestamp);

        //Assert
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM current_percentage WHERE hour = ?")) {

            stmt.setTimestamp(1, Timestamp.valueOf(timestamp));
            ResultSet rs = stmt.executeQuery();

            assertTrue(rs.next(), "Eintrag wurde nicht gefunden");

            assertEquals(75, rs.getDouble("community_depleted"));
            assertEquals(0, rs.getDouble("grid_portion"));

        }
    }

    @Test
    void ensureGridUsedPercentageIsCalculatedCorrectly() throws Exception {
        //Arrange
        LocalDateTime timestamp = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        String hourKey = timestamp.toString();

        // Simulierte PRODUCER-Nachricht
        Map<String, Object> production = Map.of(
                "type", "PRODUCER",
                "association", "COMMUNITY",
                "kwh", 10.0,
                "datetime", hourKey
        );

        // Simulierte USER-Nachricht
        Map<String, Object> usage = Map.of(
                "type", "USER",
                "association", "COMMUNITY",
                "kwh", 20.0,
                "datetime", hourKey
        );

        //Act
        handler.handleMessage(production);
        handler.handleMessage(usage);

        calculator.handleUpdate(timestamp);

        //Assert
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM current_percentage WHERE hour = ?")) {

            stmt.setTimestamp(1, Timestamp.valueOf(timestamp));
            ResultSet rs = stmt.executeQuery();

            assertTrue(rs.next(), "Eintrag wurde nicht gefunden");

            assertEquals(100, rs.getDouble("community_depleted"));
            assertEquals(50, rs.getDouble("grid_portion"));

        }
    }

    @Test
    void ensureCommunityDepletedIsZeroWhenCommunityProducedIsZero() throws Exception {
        //Arrange
        LocalDateTime timestamp = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        String hourKey = timestamp.toString();


        // Simulierte USER-Nachricht
        Map<String, Object> usage = Map.of(
                "type", "USER",
                "association", "COMMUNITY",
                "kwh", 10.0,
                "datetime", hourKey
        );

        //Act
        handler.handleMessage(usage);

        calculator.handleUpdate(timestamp);

        //Assert
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM current_percentage WHERE hour = ?")) {

            stmt.setTimestamp(1, Timestamp.valueOf(timestamp));
            ResultSet rs = stmt.executeQuery();

            assertTrue(rs.next(), "Eintrag wurde nicht gefunden");

            assertEquals(0, rs.getDouble("community_depleted"));
            assertEquals(100, rs.getDouble("grid_portion"));

        }
    }

    @Test
    void ensureGridUsedPercentageIsZeroWhenCommunityUsedIsZero() throws Exception {
        //Arrange
        LocalDateTime timestamp = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        String hourKey = timestamp.toString();

        // Simulierte PRODUCER-Nachricht
        Map<String, Object> production = Map.of(
                "type", "PRODUCER",
                "association", "COMMUNITY",
                "kwh", 10.0,
                "datetime", hourKey
        );


        //Act
        handler.handleMessage(production);

        calculator.handleUpdate(timestamp);

        //Assert
        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM current_percentage WHERE hour = ?")) {

            stmt.setTimestamp(1, Timestamp.valueOf(timestamp));
            ResultSet rs = stmt.executeQuery();

            assertTrue(rs.next(), "Eintrag wurde nicht gefunden");

            assertEquals(0, rs.getDouble("community_depleted"));
            assertEquals(0, rs.getDouble("grid_portion"));

        }
    }
}