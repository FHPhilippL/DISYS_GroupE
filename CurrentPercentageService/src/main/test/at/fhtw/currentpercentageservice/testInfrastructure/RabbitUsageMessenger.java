package at.fhtw.currentpercentageservice.testInfrastructure;

import at.fhtw.currentpercentageservice.testInterfaces.UsageMessenger;
import com.google.gson.Gson;
import com.rabbitmq.client.Channel;

import java.nio.charset.StandardCharsets;
import java.util.Map;

public class RabbitUsageMessenger implements UsageMessenger {

    private final Channel channel;
    private final String queueName;
    private final Gson gson = new Gson();

    public RabbitUsageMessenger(Channel channel, String queueName) {
        this.channel = channel;
        this.queueName = queueName;
    }

    @Override
    public void sendUsageUpdated(String hourKey) {
        try {
            String message = gson.toJson(Map.of(
                    "type", "USAGE_UPDATED",
                    "hour", hourKey
            ));

            channel.basicPublish("", queueName, null, message.getBytes(StandardCharsets.UTF_8));
            System.out.println("[→] Sent USAGE_UPDATED message to '" + queueName + "': " + message);
        } catch (Exception e) {
            System.err.println("[!] Failed to send USAGE_UPDATED message:");
            e.printStackTrace();
        }
    }
}
