package at.fhtw.currentpercentageservice;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rabbitmq.client.*;
import com.rabbitmq.client.Connection;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;

public class CurrentPercentageService {

    private static final String QUEUE_NAME = "energy.updated";
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/energy";
    private static final String DB_USER = "user";
    private static final String DB_PASS = "password";

    private static final Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        PercentageCalculator calculator= new PercentageCalculator(DB_URL, DB_USER, DB_PASS);

        // RabbitMQ Setup
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("guest");
        factory.setPassword("guest");

        Connection mqConnection = factory.newConnection();
        Channel channel = mqConnection.createChannel();

        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        System.out.println("[i] Waiting for USAGE_UPDATED messages on queue '" + QUEUE_NAME + "'...");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String messageJson = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println("[→] Received USAGE_UPDATED message: " + messageJson);

            try {
                Map<String, Object> message = gson.fromJson(messageJson, new TypeToken<Map<String, Object>>() {}.getType());
                if ("USAGE_UPDATED".equals(message.get("type"))) {
                    String hourStr = (String) message.get("hour");
                    calculator.handleUpdate(LocalDateTime.parse(hourStr));
                }
            } catch (Exception e) {
                System.err.println("[!] Error while processing message:");
                e.printStackTrace();
            }
        };

        channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {});
    }
}
