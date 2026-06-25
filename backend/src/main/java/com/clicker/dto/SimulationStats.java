package com.clicker.dto;

public record SimulationStats(
    String campaignId,
    String campaignRunId,
    int totalVisits,
    int successfulVisits,
    int failedVisits,
    int activeProxies,
    double visitsPerSecond,
    String elapsedTime,
    String estimatedRemaining
) {}
