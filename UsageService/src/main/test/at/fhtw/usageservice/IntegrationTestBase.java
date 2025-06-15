package at.fhtw.usageservice;

import at.fhtw.usageservice.infrastructure.SimpleDataSource;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.util.concurrent.TimeoutException;

@Testcontainers
public abstract class IntegrationTestBase {

    protected static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:15")
                    .withDatabaseName("energy")
                    .withUsername("user")
                    .withPassword("password");

    protected static RabbitMQContainer rabbitmq =
            new RabbitMQContainer("rabbitmq:3.12-management");

    protected static DataSource dataSource;
    protected static Channel channel;

    @BeforeAll
    static void setup() throws Exception {
        postgres.start();
        rabbitmq.start();

        // DB konfigurieren
        dataSource = new SimpleDataSource(
                postgres.getJdbcUrl(),
                postgres.getUsername(),
                postgres.getPassword()
        );

        // schema.sql ausführen
        try (java.sql.Connection conn = dataSource.getConnection();
             java.sql.Statement stmt = conn.createStatement()) {
            String sql = Files.readString(Path.of("src/main/resources/schema.sql"));
            stmt.execute(sql);
        }

        // MQ konfigurieren
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(rabbitmq.getHost());
        factory.setPort(rabbitmq.getAmqpPort());
        factory.setUsername(rabbitmq.getAdminUsername());
        factory.setPassword(rabbitmq.getAdminPassword());

        Connection mqConnection = factory.newConnection();
        channel = mqConnection.createChannel();

        channel.queueDeclare("energy.input", false, false, false, null);
        channel.queueDeclare("energy.updated", false, false, false, null);
    }

    @AfterAll
    static void tearDown() throws SQLException, IOException, TimeoutException {
        if (channel != null && channel.isOpen()) {
            channel.close();
        }
        rabbitmq.stop();
        postgres.stop();
    }
}
