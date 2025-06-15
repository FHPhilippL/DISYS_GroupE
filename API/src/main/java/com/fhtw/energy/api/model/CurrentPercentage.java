package com.fhtw.energy.api.model;

import java.time.LocalDateTime;

public class CurrentPercentage {
    public LocalDateTime hour;
    public double communityDepleted;
    public double gridPortion;

    public CurrentPercentage(LocalDateTime hour, double communityDepleted, double gridPortion) {
        this.hour = hour;
        this.communityDepleted = communityDepleted;
        this.gridPortion = gridPortion;
    }
}
