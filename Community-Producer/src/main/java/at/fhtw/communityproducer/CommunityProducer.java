package at.fhtw.communityproducer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

public class CommunityProducer {


    private static final String QUEUE_NAME = "energy.input";
    private static final Random RANDOM = new Random();
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);

    public static void main(String[] args) throws Exception {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("guest");
        factory.setPassword("guest");

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.queueDeclare(QUEUE_NAME, false, false, false, null);

            System.out.println("[i] CommunityProducerApp started.");
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

                System.out.println("[x] Sent PRODUCER: " + message);
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Schedule the next message AFTER this one completes
            scheduleNextMessage(channel);
        }, delayMillis, TimeUnit.MILLISECONDS);
    }

    public static double calculateKWh() throws Exception {
        LocalTime now = LocalTime.now();
        LocalTime sunrise = LocalTime.of(6, 0);
        LocalTime sunset = LocalTime.of(20, 0);
        //double timeOfDayFactor;

        if (now.isBefore(sunrise) || now.isAfter(sunset)) {
            return 0.0; // no production at night
        }

        WeatherAPI weatherAPI = new WeatherAPI();
        double sunlight = weatherAPI.getSunlightFactor();
        //System.out.println("sunlight: " + sunlight);

        // Combine both factors with randomness
        double base = 0.001;
        double range = 0.004;
        double kwh = base + (RANDOM.nextDouble() * range * sunlight);
        //System.out.println("kWh: " + kwh);

        return Math.round(kwh * 1000.0) / 1000.0; // round to 3 digits
    }

}
