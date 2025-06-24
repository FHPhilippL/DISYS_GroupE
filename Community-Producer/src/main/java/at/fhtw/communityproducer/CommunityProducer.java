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

public class CommunityProducer {

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
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
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
                }
            }, 0, 5000);

            System.out.println("[i] CommunityProducerApp started.");
            Thread.currentThread().join();
        }
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
