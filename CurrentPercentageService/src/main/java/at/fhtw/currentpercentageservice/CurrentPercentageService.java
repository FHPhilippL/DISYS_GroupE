package at.fhtw.currentpercentageservice;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.rabbitmq.client.*;
import com.rabbitmq.client.Connection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

public class CurrentPercentageService {

    private static final Logger logger = LoggerFactory.getLogger(CurrentPercentageService.class);

    private static final String QUEUE_NAME = "energy.updated";
    private static final String DB_URL = "jdbc:postgresql://localhost:5432/energy";
    private static final String DB_USER = "user";
    private static final String DB_PASS = "password";

    private static final Gson gson = new Gson();

    public static void main(String[] args) throws Exception {
        logger.info("[i] Current Percentage Service Started");
        PercentageCalculator calculator= new PercentageCalculator(DB_URL, DB_USER, DB_PASS);

        // RabbitMQ Setup
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setUsername("guest");
        factory.setPassword("guest");

        Channel channel = null;

        //trys to connect to the RabbitMQ, fails if the docker container is not started
        try{
            Connection mqConnection = factory.newConnection();
            channel = mqConnection.createChannel();
        } catch (IOException | TimeoutException e) {
            logger.error("Could not connect to RabbitMQ: {}", e.getMessage());
        }

        // if the container has not been created, stops the programm
        if (channel == null) {
            logger.error("Channel not created");
            return;
        }


        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        logger.info("[i] Waiting for USAGE_UPDATED messages on queue '" + QUEUE_NAME + "'...");

        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String messageJson = new String(delivery.getBody(), StandardCharsets.UTF_8);
            logger.info("[→] Received USAGE_UPDATED message: {}",  messageJson);

            try {
                Map<String, Object> message = gson.fromJson(messageJson, new TypeToken<Map<String, Object>>() {}.getType());
                if ("USAGE_UPDATED".equals(message.get("type"))) {
                    String hourStr = (String) message.get("hour");
                    calculator.handleUpdate(LocalDateTime.parse(hourStr));
                }
            } catch (Exception e) {
                logger.error("[!] Error while processing message: {}", e.getMessage());
            }
        };

        channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {});
    }
}
