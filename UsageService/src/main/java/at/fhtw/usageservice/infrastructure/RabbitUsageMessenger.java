package at.fhtw.usageservice.infrastructure;

import at.fhtw.usageservice.interfaces.UsageMessenger;
import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * RabbitMQ-based implementation of the {@link UsageMessenger} interface.
 * Sends notification messages to a configured RabbitMQ queue when usage data is updated.
 */
public class RabbitUsageMessenger implements UsageMessenger {

    private static final Logger logger = LoggerFactory.getLogger(RabbitUsageMessenger.class);

    private final Channel channel;
    private final String queueName;
    private final Gson gson = new Gson();

    /**
     * Constructs the messenger with a given RabbitMQ channel and queue name.
     *
     * @param channel   the RabbitMQ channel used for publishing messages
     * @param queueName the name of the queue to which messages should be sent
     */
    public RabbitUsageMessenger(Channel channel, String queueName) {
        this.channel = channel;
        this.queueName = queueName;
    }

    /**
     * Sends a "USAGE_UPDATED" message with the associated hour key.
     * The message is serialized to JSON and delivered to the specified RabbitMQ queue.
     *
     * @param hourKey the hour for which usage was updated, formatted as ISO datetime string
     */
    @Override
    public void sendUsageUpdated(String hourKey) {
        try {
            // Create JSON message content
            String message = gson.toJson(Map.of(
                    "type", "USAGE_UPDATED",
                    "hour", hourKey
            ));

            // Publish message to the default exchange and target queue
            channel.basicPublish("", queueName, null, message.getBytes(StandardCharsets.UTF_8));
            logger.info("Sent USAGE_UPDATED message to '{}': {}", queueName, message);
        } catch (Exception e) {
            logger.error("Failed to send USAGE_UPDATED message", e);
        }
    }
}
