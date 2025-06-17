package at.fhtw.currentpercentageservice;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rabbitmq.client.*;
import com.rabbitmq.client.Connection;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.Map;

public class CurrentPercentageService {

    private static final String QUEUE_NAME = "energy.updated";
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/energy";
    private static final String DB_USER = "user";
    private static final String DB_PASS = "password";

    private static final Gson gson = new Gson();

    public static void main(String[] args) throws Exception {

        // RabbitMQ Setup
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("guest");
        factory.setPassword("guest");

        Connection mqConnection = factory.newConnection();
        Channel channel = mqConnection.createChannel();

        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        System.out.println("[i] Waiting for USAGE_UPDATED messages on queue '" + QUEUE_NAME + "'...");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String messageJson = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println("[→] Received USAGE_UPDATED message: " + messageJson);

            try {
                Map<String, Object> message = gson.fromJson(messageJson, new TypeToken<Map<String, Object>>() {}.getType());
                if ("USAGE_UPDATED".equals(message.get("type"))) {
                    String hourStr = (String) message.get("hour");
                    handleUpdate(LocalDateTime.parse(hourStr));
                }
            } catch (Exception e) {
                System.err.println("[!] Error while processing message:");
                e.printStackTrace();
            }
        };

        channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {});
    }

    private static void handleUpdate(LocalDateTime hour) {
        try (java.sql.Connection db = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            PreparedStatement query = db.prepareStatement(
                    "SELECT community_produced, community_used, grid_used FROM usage_hourly WHERE hour = ?"
            );
            query.setTimestamp(1, Timestamp.valueOf(hour));
            ResultSet rs = query.executeQuery();

            if (rs.next()) {
                double communityProduced = rs.getDouble("community_produced");
                double communityUsed = rs.getDouble("community_used");
                double gridUsed = rs.getDouble("grid_used");
                double totalUsed = communityUsed + gridUsed;

                double communityDepleted = (communityProduced > 0) ? (communityUsed<=communityProduced) ?
                        (communityUsed/communityProduced) * 100 : 100 : 0;
                double gridPortion = (totalUsed > 0) ? (gridUsed / totalUsed) * 100 : 0;

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
