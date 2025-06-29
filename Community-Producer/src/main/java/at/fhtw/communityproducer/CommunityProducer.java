package at.fhtw.communityproducer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class CommunityProducer {

    private static final String QUEUE_NAME = "energy.input";
    private static final Random RANDOM = new Random();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private static final Logger logger = LoggerFactory.getLogger(CommunityProducer.class);

    public static void main(String[] args) throws Exception {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("guest");
        factory.setPassword("guest");

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.queueDeclare(QUEUE_NAME, false, false, false, null);

            logger.info("[i] CommunityProducerApp started.");
            scheduleNextMessage(channel);
            Thread.currentThread().join();
        }
    }

    private static void scheduleNextMessage(Channel channel) {
        int delayMillis = ThreadLocalRandom.current().nextInt(1000, 5001);

        scheduler.schedule(() -> {
            try {
                double kwh = calculateKWh();
                String message = String.format(java.util.Locale.US,
                        "{\"type\":\"PRODUCER\",\"association\":\"COMMUNITY\",\"kwh\":%.3f,\"datetime\":\"%s\"}",
                        kwh, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));

                channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));

                logger.info("Sent PRODUCER: " + message);
            } catch (Exception e) {
                logger.error("Failed to send message", e);
            }

            // Schedule the next message AFTER this one completes
            scheduleNextMessage(channel);
        }, delayMillis, TimeUnit.MILLISECONDS);
    }

    public static double calculateKWh() {
        WeatherAPI weatherAPI = new WeatherAPI();
        double sunlight = weatherAPI.getSunlightFactor();

        if (WeatherAPI.isSunShining()) {
            return 0.0; // no production at night
        }

        logger.debug("Sunlightfactor: " + sunlight);

        // Combine both factors with randomness
        double base = 0.001;
        double range = 0.004;
        double kwh = base + (RANDOM.nextDouble() * range * sunlight);
        logger.debug("kWh: " + kwh);

        return Math.round(kwh * 1000.0) / 1000.0; // round to 3 digits
    }

}
