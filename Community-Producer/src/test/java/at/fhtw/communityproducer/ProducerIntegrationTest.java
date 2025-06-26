package at.fhtw.communityproducer;

import com.rabbitmq.client.*;
import org.junit.*;
import org.testcontainers.containers.RabbitMQContainer;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import static at.fhtw.communityproducer.CommunityProducer.calculateKWh;
import static org.junit.Assert.*;

public class ProducerIntegrationTest {

    private static final String QUEUE = "energy.input";

    private static RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.12-management");
    private static Connection connection;
    private static Channel channel;

    @BeforeClass
    public static void setup() throws Exception {
        rabbitmq.start();

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitmq.getHost());
        factory.setPort(rabbitmq.getAmqpPort());
        factory.setUsername(rabbitmq.getAdminUsername());
        factory.setPassword(rabbitmq.getAdminPassword());

        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.queueDeclare(QUEUE, false, false, false, null);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        channel.close();
        connection.close();
        rabbitmq.stop();
    }

    @Test
    public void testMessageIsPublishedToQueue() throws Exception {
        double kwh = calculateKWh();
        String msg = String.format(java.util.Locale.US,
                "{\"type\":\"PRODUCER\",\"association\":\"COMMUNITY\",\"kwh\":%.3f,\"datetime\":\"%s\"}",
                kwh, LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS));

        channel.basicPublish("", QUEUE, null, msg.getBytes(StandardCharsets.UTF_8));

        GetResponse response = channel.basicGet(QUEUE, true);
        assertNotNull("Message should be received from queue", response);

        String body = new String(response.getBody(), StandardCharsets.UTF_8);
        assertTrue("Message should contain type", body.contains("\"type\":\"PRODUCER\""));
        assertTrue("Message should contain kwh", body.contains("\"kwh\":"));
    }
}
