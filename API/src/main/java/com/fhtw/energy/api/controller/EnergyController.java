package com.fhtw.energy.api.controller;

import com.fhtw.energy.api.model.CurrentPercentage;
import com.fhtw.energy.api.model.HourlyUsage;
import com.fhtw.energy.api.model.UsageSummary;
import com.fhtw.energy.api.service.EnergyService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/energy")
public class EnergyController {

    private final EnergyService energyService;

    public EnergyController(EnergyService energyService) {
        this.energyService = energyService;
    }

    @GetMapping("/current")
    public CurrentPercentage getCurrent() {
        return energyService.getCurrentPercentage();
    }

    @GetMapping("/historical")
    public UsageSummary getHistorical(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return energyService.getHistorical(start, end);
    }

    @GetMapping("/historical-detailed")
    public List<HourlyUsage> getHistoricalDetailed(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        return energyService.getHistoricalDetailed(start, end);
    }

}
