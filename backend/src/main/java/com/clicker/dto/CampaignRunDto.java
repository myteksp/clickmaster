package com.clicker.dto;

import com.clicker.domain.RunStatus;
import java.time.Instant;
import java.util.UUID;

public record CampaignRunDto(
    UUID id,
    UUID campaignId,
    RunStatus status,
    Instant startedAt,
    Instant finishedAt,
    int totalVisits,
    int successfulVisits,
    int failedVisits,
    String stats
) {}
