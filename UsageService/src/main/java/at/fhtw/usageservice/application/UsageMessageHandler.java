package at.fhtw.usageservice.application;

import at.fhtw.usageservice.interfaces.UsageMessenger;
import at.fhtw.usageservice.interfaces.UsageRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

public class UsageMessageHandler {

    private final UsageRepository repository;
    private final UsageMessenger messenger;

    private final Map<String, Double> hourlyProduced = new HashMap<>();
    private final Map<String, Double> hourlyUsed = new HashMap<>();
    private final Map<String, Double> hourlyGrid = new HashMap<>();

    public UsageMessageHandler(UsageRepository repository, UsageMessenger messenger) {
        this.repository = repository;
        this.messenger = messenger;
    }

    public void handleMessage(Map<String, Object> message) {
        String type = (String) message.get("type");
        String association = (String) message.get("association");

        if (!"COMMUNITY".equalsIgnoreCase(association)) {
            System.out.println("[!] Ignoring message with non-community association.");
            return;
        }

        double kwh = ((Number) message.get("kwh")).doubleValue();
        String datetimeStr = (String) message.get("datetime");

        LocalDateTime dt = LocalDateTime.parse(datetimeStr);
        LocalDateTime hour = dt.truncatedTo(ChronoUnit.HOURS);
        String hourKey = hour.toString();

        if ("PRODUCER".equalsIgnoreCase(type)) {
            hourlyProduced.put(hourKey, hourlyProduced.getOrDefault(hourKey, 0.0) + kwh);
        } else if ("USER".equalsIgnoreCase(type)) {
            hourlyUsed.put(hourKey, hourlyUsed.getOrDefault(hourKey, 0.0) + kwh);
        }

        double produced = hourlyProduced.getOrDefault(hourKey, 0.0);
        double used = hourlyUsed.getOrDefault(hourKey, 0.0);
        double grid = hourlyGrid.getOrDefault(hourKey, 0.0);

        double netDeficit = used - produced;
        double updatedGrid = Math.max(grid, Math.max(0, netDeficit));
        hourlyGrid.put(hourKey, updatedGrid);

        // Persist to DB
        repository.saveHourlyUsage(hour, produced, used, grid);

        // Notify via MQ
        messenger.sendUsageUpdated(hourKey);

        System.out.printf("[✓] %s → %.3f kWh → Hour: %s | Produced=%.3f Used=%.3f Grid=%.3f%n",
                type, kwh, hourKey, produced, used, grid);
    }
}
