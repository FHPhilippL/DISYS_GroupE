package com.fhtw.energy.api.model;

import java.time.LocalDateTime;

public class HourlyUsage {
    private LocalDateTime hour;
    private double communityProduced;
    private double communityUsed;

    public HourlyUsage(LocalDateTime hour, double communityProduced, double communityUsed) {
        this.hour = hour;
        this.communityProduced = communityProduced;
        this.communityUsed = communityUsed;
    }

    public LocalDateTime getHour() {
        return hour;
    }

    public void setHour(LocalDateTime hour) {
        this.hour = hour;
    }

    public double getCommunityProduced() {
        return communityProduced;
    }

    public void setCommunityProduced(double communityProduced) {
        this.communityProduced = communityProduced;
    }

    public double getCommunityUsed() {
        return communityUsed;
    }

    public void setCommunityUsed(double communityUsed) {
        this.communityUsed = communityUsed;
    }
}