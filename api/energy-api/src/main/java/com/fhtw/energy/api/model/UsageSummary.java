package com.fhtw.energy.api.model;

public class UsageSummary {
    public double totalCommunityProduced;
    public double totalCommunityUsed;
    public double totalGridUsed;

    public UsageSummary(double totalCommunityProduced, double totalCommunityUsed, double totalGridUsed) {
        this.totalCommunityProduced = totalCommunityProduced;
        this.totalCommunityUsed = totalCommunityUsed;
        this.totalGridUsed = totalGridUsed;
    }
}
