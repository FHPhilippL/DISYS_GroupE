package com.fhtw.energy.api.controller;

import com.fhtw.energy.api.model.CurrentPercentage;
import com.fhtw.energy.api.model.UsageHour;
import com.fhtw.energy.api.service.EnergyService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/energy")
public class EnergyController {
    private final EnergyService service;

    public EnergyController(EnergyService service) {
        this.service = service;
    }

    @GetMapping("/current")
    public CurrentPercentage getCurrentPercentage() {
        return service.getCurrent();
    }

    @GetMapping("/historical")
    public List<UsageHour> getHistorical(
            @RequestParam("start") String start,
            @RequestParam("end") String end
    ) {
        return service.getHistorical(LocalDateTime.parse(start), LocalDateTime.parse(end));
    }
}
