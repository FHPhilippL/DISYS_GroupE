package com.fhtw.energy.api.service;

import com.fhtw.energy.api.model.CurrentPercentage;
import com.fhtw.energy.api.model.UsageHour;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class EnergyService {

    private final List<UsageHour> usageData = List.of(
            new UsageHour(LocalDateTime.of(2025, 1, 10, 13, 0), 15.015, 14.033, 2.049),
            new UsageHour(LocalDateTime.of(2025, 1, 10, 14, 0), 18.05, 18.05, 1.076),
            new UsageHour(LocalDateTime.of(2025, 1, 10, 15, 0), 16.1, 17.4, 1.9)
    );

    private final CurrentPercentage current = new CurrentPercentage(
            LocalDateTime.of(2025, 1, 10, 14, 0),
            100.0,
            5.63
    );

    public List<UsageHour> getHistorical(LocalDateTime start, LocalDateTime end) {
        return usageData.stream()
                .filter(u -> !u.hour.isBefore(start) && !u.hour.isAfter(end))
                .collect(Collectors.toList());
    }

    public CurrentPercentage getCurrent() {
        return current;
    }
}
