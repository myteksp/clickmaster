package com.clicker.dto;

import com.clicker.domain.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record CampaignCreateRequest(
    @NotNull UUID siteId,
    @NotBlank String name,
    SimulationLevel simulationLevel,
    TrafficPattern trafficPattern,
    int visitsPerHour,
    int durationMinutes,
    String scheduleCron,
    List<GeoDistributionDto> geoDistribution,
    List<DeviceProfileDto> deviceProfile,
    UserAgentConfigDto userAgentConfig,
    ProxyConfigDto proxyConfig,
    List<ClickTargetDto> clickTargets,
    List<CampaignScenarioLink> scenarios
) {}
