package at.fhtw.communityuser;

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

public class CommunityUser {

    private static final String QUEUE_NAME = "energy.input";
    private static final Random RANDOM = new Random();

    public static void main(String[] args) throws Exception {

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("guest");
        factory.setPassword("guest");

        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {

            channel.queueDeclare(QUEUE_NAME, false, false, false, null);

            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        double kwh = calculateKWh();
                        //double kwh = 0.002 + (0.004 * RANDOM.nextDouble());
                        String message = String.format(java.util.Locale.US,
                                "{\"type\":\"USER\",\"association\":\"COMMUNITY\",\"kwh\":%.3f,\"datetime\":\"%s\"}",
                                kwh, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));

                        channel.basicPublish("", QUEUE_NAME, null, message.getBytes(StandardCharsets.UTF_8));
                        System.out.println("[x] Sent USER: " + message);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }, 0, 5000);

            System.out.println("[i] CommunityUserApp started.");
            Thread.currentThread().join();
        }
    }

    public static double calculateKWh() {
        LocalTime now = LocalTime.now();
        double timeOfDayFactor;

        // Define peak usage times
        LocalTime morningPeakStart = LocalTime.of(6, 0);
        LocalTime morningPeakEnd = LocalTime.of(9, 0);
        LocalTime eveningPeakStart = LocalTime.of(17, 0);
        LocalTime eveningPeakEnd = LocalTime.of(21, 0);

        if (now.isBefore(LocalTime.of(5, 0)) || now.isAfter(LocalTime.of(23, 0))) {
            return 0.0005; // Almost no usage late at night, but still some
        }

        // Determine time-based usage factor
        if ((now.isAfter(morningPeakStart) && now.isBefore(morningPeakEnd)) ||
                (now.isAfter(eveningPeakStart) && now.isBefore(eveningPeakEnd))) {
            timeOfDayFactor = 1.0; // Peak usage
        } else if (now.isAfter(LocalTime.of(12, 0)) && now.isBefore(LocalTime.of(16, 0))) {
            timeOfDayFactor = 0.3; // Low midday usage
        } else {
            timeOfDayFactor = 0.6; // Moderate usage
        }

        //System.out.println("timeOfDayFactor (usage): " + timeOfDayFactor);

        // Define base and range for user consumption
        double base = 0.002;     // base minimal usage
        double range = 0.004;    // max variability

        double kwh = base + (RANDOM.nextDouble() * range * timeOfDayFactor);
        //System.out.println("User kWh usage: " + kwh);

        return Math.round(kwh * 1000.0) / 1000.0;
    }


}
