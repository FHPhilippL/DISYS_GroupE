package at.fhtw.usageservice;

import at.fhtw.usageservice.application.UsageMessageHandler;
import at.fhtw.usageservice.infrastructure.JdbcUsageRepository;
import at.fhtw.usageservice.infrastructure.RabbitUsageMessenger;
import at.fhtw.usageservice.infrastructure.SimpleDataSource;
import at.fhtw.usageservice.interfaces.UsageMessenger;
import at.fhtw.usageservice.interfaces.UsageRepository;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rabbitmq.client.*;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class UsageServiceApplication {

    private static final String INPUT_QUEUE = "energy.input";
    private static final String UPDATE_QUEUE = "energy.updated";

    private static final String DB_URL = "jdbc:postgresql://localhost:5432/energy";
    private static final String DB_USER = "user";
    private static final String DB_PASS = "password";

    public static void main(String[] args) throws Exception {
        // 1. RabbitMQ-Verbindung
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("guest");
        factory.setPassword("guest");

        Connection rabbitConnection = factory.newConnection();
        Channel channel = rabbitConnection.createChannel();

        channel.queueDeclare(INPUT_QUEUE, false, false, false, null);
        channel.queueDeclare(UPDATE_QUEUE, false, false, false, null);

        // 2. DataSource (einfache manuelle Konfiguration)
        DataSource dataSource = new SimpleDataSource(DB_URL, DB_USER, DB_PASS);

        // 3. Abhängigkeiten einbinden
        UsageRepository repository = new JdbcUsageRepository(dataSource);
        UsageMessenger messenger = new RabbitUsageMessenger(channel, UPDATE_QUEUE);
        UsageMessageHandler service = new UsageMessageHandler(repository, messenger);

        Gson gson = new Gson();

        // 4. Nachrichtenkonsum starten
        System.out.println("[✓] Waiting for messages on '" + INPUT_QUEUE + "'...");

        DeliverCallback callback = (tag, delivery) -> {
            String json = new String(delivery.getBody(), StandardCharsets.UTF_8);
            try {
                Map<String, Object> message = gson.fromJson(json, new TypeToken<Map<String, Object>>() {}.getType());
                service.handleMessage(message);
            } catch (Exception e) {
                System.err.println("[!] Failed to handle message:");
                e.printStackTrace();
            }
        };

        channel.basicConsume(INPUT_QUEUE, true, callback, tag -> {});

        // Keep running
        Thread.currentThread().join();
    }
}
