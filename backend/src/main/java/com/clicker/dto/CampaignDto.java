package com.clicker.dto;

import com.clicker.domain.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record CampaignDto(
    UUID id,
    UUID userId,
    UUID siteId,
    String siteName,
    String siteBaseUrl,
    String name,
    CampaignStatus status,
    SimulationLevel simulationLevel,
    TrafficPattern trafficPattern,
    int visitsPerHour,
    int durationMinutes,
    String scheduleCron,
    List<GeoDistributionDto> geoDistribution,
    List<DeviceProfileDto> deviceProfile,
    UserAgentConfigDto userAgentConfig,
    ProxyConfigDto proxyConfig,
    List<CampaignScenarioDto> scenarios,
    Instant createdAt,
    Instant updatedAt,
    Instant lastRunAt
) {}
