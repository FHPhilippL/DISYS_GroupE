package com.fhtw.energy.api.model;

import java.time.LocalDateTime;

public class UsageHour {
    public LocalDateTime hour;
    public double communityProduced;
    public double communityUsed;
    public double gridUsed;

    public UsageHour(LocalDateTime hour, double communityProduced, double communityUsed, double gridUsed) {
        this.hour = hour;
        this.communityProduced = communityProduced;
        this.communityUsed = communityUsed;
        this.gridUsed = gridUsed;
    }
}
