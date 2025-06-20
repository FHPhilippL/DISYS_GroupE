package com.fhtw.energy.api.controller;

import com.fhtw.energy.api.model.CurrentPercentage;
import com.fhtw.energy.api.model.UsageSummary;
import com.fhtw.energy.api.service.EnergyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;

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
            @RequestParam("start") String start,
            @RequestParam("end") String end
    ) {
        return energyService.getHistorical(LocalDateTime.parse(start), LocalDateTime.parse(end));
    }
}
