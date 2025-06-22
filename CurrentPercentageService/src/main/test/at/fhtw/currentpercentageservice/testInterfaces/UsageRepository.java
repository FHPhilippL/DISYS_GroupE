package at.fhtw.currentpercentageservice.testInterfaces;

import java.time.LocalDateTime;

public interface UsageRepository {
    void saveHourlyUsage(LocalDateTime hour, double produced, double used, double grid);
}