package com.fhtw.energy.api.controller;

import com.fhtw.energy.api.model.CurrentPercentage;
import com.fhtw.energy.api.model.HourlyUsage;
import com.fhtw.energy.api.model.UsageSummary;
import com.fhtw.energy.api.service.EnergyService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * REST Controller for energy usage data.
 *
 * This controller provides three main API endpoints:
 *
 * 1. GET /energy/current
 *    → Returns the current community energy depletion and grid usage percentage.
 *
 * 2. GET /energy/historical?start=...&end=...
 *    → Returns a summarized view (totals) of energy production and consumption
 *      for a given time range.
 *
 * 3. GET /energy/historical-detailed?start=...&end=...
 *    → Returns detailed hourly data (per hour values) of community usage and production.
 *
 * All endpoints log incoming requests using SLF4J.
 * This controller delegates business logic to the EnergyService.
 */
@RestController
@RequestMapping("/energy")
public class EnergyController {

    private static final Logger logger = LoggerFactory.getLogger(EnergyController.class);

    private final EnergyService energyService;

    public EnergyController(EnergyService energyService) {
        this.energyService = energyService;
    }

    /**
     * Endpoint to retrieve the current usage status.
     *
     * @return current usage percentages (community and grid)
     */
    @GetMapping("/current")
    public CurrentPercentage getCurrent() {
        logger.info("GET /energy/current called");
        return energyService.getCurrentPercentage();
    }

    /**
     * Endpoint to retrieve total usage values within a date/time range.
     *
     * @param start formatted start timestamp
     * @param end formatted end timestamp
     * @return total energy usage and production in the specified period
     */
    @GetMapping("/historical")
    public UsageSummary getHistorical(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        logger.info("GET /energy/historical called with start={} and end={}", start, end);
        return energyService.getHistorical(start, end);
    }

    /**
     * Endpoint to retrieve detailed hourly usage data within a date/time range.
     *
     * @param start  formatted start timestamp
     * @param end formatted end timestamp
     * @return list of hourly energy usage records for the specified period
     */
    @GetMapping("/historical-detailed")
    public List<HourlyUsage> getHistoricalDetailed(
            @RequestParam("start") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @RequestParam("end") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime end
    ) {
        logger.info("GET /energy/historical-detailed called with start={} and end={}", start, end);
        return energyService.getHistoricalDetailed(start, end);
    }

}
