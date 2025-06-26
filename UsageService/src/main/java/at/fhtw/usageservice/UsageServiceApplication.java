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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Entry point of the Usage Service.
 *
 * This service listens for energy usage and production events from RabbitMQ,
 * aggregates and stores them into a PostgreSQL database, and sends update notifications
 * when new hourly data is persisted.
 */
public class UsageServiceApplication {

    private static final Logger logger = LoggerFactory.getLogger(UsageServiceApplication.class);

    // Queue names for incoming events and outgoing notifications
    private static final String INPUT_QUEUE = "energy.input";
    private static final String UPDATE_QUEUE = "energy.updated";

    // Database connection credentials
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/energy";
    private static final String DB_USER = "user";
    private static final String DB_PASS = "password";

    /**
     * Initializes the application and starts consuming messages from RabbitMQ.
     */
    public static void main(String[] args) throws Exception {
        // 1. Connect to RabbitMQ
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("guest");
        factory.setPassword("guest");

        Connection rabbitConnection = factory.newConnection();
        Channel channel = rabbitConnection.createChannel();

        // Declare the queues used by this service
        channel.queueDeclare(INPUT_QUEUE, false, false, false, null);
        channel.queueDeclare(UPDATE_QUEUE, false, false, false, null);

        // 2. Set up database connection using a simple custom DataSource
        DataSource dataSource = new SimpleDataSource(DB_URL, DB_USER, DB_PASS);

        // 3. Wire up service dependencies (repository, messenger, message handler)
        UsageRepository repository = new JdbcUsageRepository(dataSource);
        UsageMessenger messenger = new RabbitUsageMessenger(channel, UPDATE_QUEUE);
        UsageMessageHandler service = new UsageMessageHandler(repository, messenger);

        Gson gson = new Gson();

        // 4. Start listening for incoming messages
        logger.info("Waiting for messages on '{}'", INPUT_QUEUE);

        DeliverCallback callback = (tag, delivery) -> {
            String json = new String(delivery.getBody(), StandardCharsets.UTF_8);
            try {
                Map<String, Object> message = gson.fromJson(json, new TypeToken<Map<String, Object>>() {}.getType());
                service.handleMessage(message);  // Delegate processing to the business logic
            } catch (Exception e) {
                logger.error("Failed to handle message", e);
            }
        };

        // Start consuming messages continuously
        channel.basicConsume(INPUT_QUEUE, true, callback, tag -> {});

        // Prevent the application from exiting
        Thread.currentThread().join();
    }
}
