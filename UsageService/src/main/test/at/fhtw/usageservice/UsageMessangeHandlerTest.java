package at.fhtw.usageservice;

import at.fhtw.usageservice.application.UsageMessageHandler;
import at.fhtw.usageservice.infrastructure.JdbcUsageRepository;
import at.fhtw.usageservice.infrastructure.RabbitUsageMessenger;
import at.fhtw.usageservice.interfaces.UsageMessenger;
import at.fhtw.usageservice.interfaces.UsageRepository;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rabbitmq.client.GetResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

// Diese Testklasse testet die Verarbeitung von PRODUCER- und USER-Nachrichten
// inklusive Datenbankspeicherung und RabbitMQ-Publishing.
class UsageMessageHandlerTest extends IntegrationTestBase {

    private UsageMessageHandler handler; // Der Service, der getestet wird
    private final Gson gson = new Gson(); // Für JSON-Verarbeitung der Nachrichten

    /**
     * Wird vor jedem einzelnen Test aufgerufen.
     * Baut den Handler mit echter RabbitMQ- und Datenbankanbindung.
     */
    @BeforeEach
    void setupHandler() {
        // Repository mit echter (Testcontainer-)Datenbank
        UsageRepository repo = new JdbcUsageRepository(dataSource);

        // Messenger für RabbitMQ mit Queue "energy.updated"
        UsageMessenger messenger = new RabbitUsageMessenger(channel, "energy.updated");

        // Handler zusammenbauen (SUT = System Under Test)
        handler = new UsageMessageHandler(repo, messenger);
    }

    /**
     * Testet: Eine PRODUCER-Nachricht wird korrekt verarbeitet:
     * - Sie wird in der DB gespeichert
     * - Eine Update-Nachricht wird über RabbitMQ gesendet
     */
    @Test
    void testProducerMessage_persistsAndSendsUpdate() throws Exception {
        // =======================
        // Arrange = Vorbereitung
        // =======================

        // Aktuelle Stunde exakt (ohne Minuten/Sekunden/Nanos)
        LocalDateTime timestamp = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        String hourKey = timestamp.toString();

        // Simulierte PRODUCER-Nachricht
        Map<String, Object> message = Map.of(
                "type", "PRODUCER",
                "association", "COMMUNITY",
                "kwh", 12.5,
                "datetime", hourKey
        );

        // =============
        // Act = Aktion
        // =============

        handler.handleMessage(message);

        // ======================
        // Assert = Überprüfung DB
        // ======================

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM usage_hourly WHERE hour = ?")) {

            stmt.setTimestamp(1, Timestamp.valueOf(timestamp));
            ResultSet rs = stmt.executeQuery();

            // Ein Eintrag muss da sein
            assertTrue(rs.next(), "Eintrag wurde nicht gefunden");

            // Erwartete Werte prüfen
            assertEquals(12.5, rs.getDouble("community_produced"), 0.01);
            assertEquals(0.0, rs.getDouble("community_used"), 0.01);
            assertEquals(0.0, rs.getDouble("grid_used"), 0.01);
        }

        // ======================
        // Assert = Überprüfung MQ
        // ======================

        GetResponse response = channel.basicGet("energy.updated", true); // Nachricht holen
        assertNotNull(response, "Keine Nachricht in energy.updated");

        // JSON-Nachricht analysieren
        String body = new String(response.getBody());
        Map<String, Object> event = gson.fromJson(body, new TypeToken<Map<String, Object>>(){}.getType());

        assertEquals("USAGE_UPDATED", event.get("type"));
        assertEquals(hourKey, event.get("hour"));
    }

    /**
     * Testet: Eine USER-Nachricht wird korrekt verarbeitet:
     * - community_used wird gespeichert
     * - grid_used ist gleich used (weil produziert = 0)
     * - RabbitMQ erhält Update-Event
     */
    @Test
    void testUserMessage_persistsAndSendsUpdate() throws Exception {
        // =======================
        // Arrange = Vorbereitung
        // =======================

        LocalDateTime timestamp = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        String hourKey = timestamp.toString();

        // Simulierte USER-Nachricht
        Map<String, Object> message = Map.of(
                "type", "USER",
                "association", "COMMUNITY",
                "kwh", 7.25,
                "datetime", hourKey
        );

        // =============
        // Act = Aktion
        // =============

        handler.handleMessage(message);

        // ======================
        // Assert = Überprüfung DB
        // ======================

        try (Connection conn = dataSource.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM usage_hourly WHERE hour = ?")) {

            stmt.setTimestamp(1, Timestamp.valueOf(timestamp));
            ResultSet rs = stmt.executeQuery();

            assertTrue(rs.next(), "Eintrag wurde nicht gefunden");

            assertEquals(0.0, rs.getDouble("community_produced"), 0.01);
            assertEquals(7.25, rs.getDouble("community_used"), 0.01);
            assertEquals(7.25, rs.getDouble("grid_used"), 0.01); // weil: produced = 0
        }

        // ======================
        // Assert = Überprüfung MQ
        // ======================

        GetResponse response = channel.basicGet("energy.updated", true);
        assertNotNull(response, "Keine Nachricht in energy.updated");

        String body = new String(response.getBody());
        Map<String, Object> event = gson.fromJson(body, new TypeToken<Map<String, Object>>(){}.getType());

        assertEquals("USAGE_UPDATED", event.get("type"));
        assertEquals(hourKey, event.get("hour"));
    }
}