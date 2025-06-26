package at.fhtw.usageservice.application;

import at.fhtw.usageservice.interfaces.UsageMessenger;
import at.fhtw.usageservice.interfaces.UsageRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handles incoming usage messages for the "COMMUNITY" association.
 * This class aggregates energy usage and production on an hourly basis.
 * It stores the data to the repository and notifies other services via a messaging system.
 */
public class UsageMessageHandler {

    private final UsageRepository repository;
    private final UsageMessenger messenger;
    private static final Logger logger = LoggerFactory.getLogger(UsageMessageHandler.class);

    // Temporary in-memory maps to accumulate hourly data
    private final Map<String, Double> hourlyProduced = new HashMap<>();
    private final Map<String, Double> hourlyUsed = new HashMap<>();
    private final Map<String, Double> hourlyGrid = new HashMap<>();

    /**
     * Constructs a new handler for usage messages.
     *
     * @param repository the storage mechanism used to persist aggregated hourly usage
     * @param messenger  the messaging component used to broadcast updates
     */
    public UsageMessageHandler(UsageRepository repository, UsageMessenger messenger) {
        this.repository = repository;
        this.messenger = messenger;
    }

    /**
     * Processes a single incoming usage message.
     * Only handles messages associated with "COMMUNITY".
     * Aggregates data by hour and persists the result.
     * Also sends an update notification for the given hour.
     *
     * @param message the incoming message containing usage data
     */
    public void handleMessage(Map<String, Object> message) {
        String type = (String) message.get("type");
        String association = (String) message.get("association");

        // Skip messages that do not belong to the "COMMUNITY" group
        if (!"COMMUNITY".equalsIgnoreCase(association)) {
            logger.warn("Ignoring message with non-community association: {}", association);
            return;
        }

        double kwh = ((Number) message.get("kwh")).doubleValue();
        String datetimeStr = (String) message.get("datetime");

        // Truncate datetime to the hour for aggregation
        LocalDateTime dt = LocalDateTime.parse(datetimeStr);
        LocalDateTime hour = dt.truncatedTo(ChronoUnit.HOURS);
        String hourKey = hour.toString();

        // Accumulate production or usage based on message type
        if ("PRODUCER".equalsIgnoreCase(type)) {
            hourlyProduced.put(hourKey, hourlyProduced.getOrDefault(hourKey, 0.0) + kwh);
        } else if ("USER".equalsIgnoreCase(type)) {
            hourlyUsed.put(hourKey, hourlyUsed.getOrDefault(hourKey, 0.0) + kwh);
        }

        // Calculate net grid usage
        double produced = hourlyProduced.getOrDefault(hourKey, 0.0);
        double used = hourlyUsed.getOrDefault(hourKey, 0.0);
        double grid = hourlyGrid.getOrDefault(hourKey, 0.0);

        double netDeficit = used - produced;
        double updatedGrid = Math.max(grid, Math.max(0, netDeficit));
        hourlyGrid.put(hourKey, updatedGrid);
        grid = hourlyGrid.getOrDefault(hourKey, 0.0);

        // Persist the aggregated values to the database
        repository.saveHourlyUsage(hour, produced, used, grid);

        // Send an update message (e.g., via RabbitMQ)
        messenger.sendUsageUpdated(hourKey);

        // Log the processed message
        logger.info("{} → {} kWh → Hour: {} | Produced={} Used={} Grid={}",
                type, String.format("%.3f", kwh), hourKey,
                String.format("%.3f", produced),
                String.format("%.3f", used),
                String.format("%.3f", grid));
    }

}
