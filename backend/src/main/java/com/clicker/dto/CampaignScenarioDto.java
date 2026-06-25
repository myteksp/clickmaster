package com.clicker.dto;

import java.util.UUID;

public record CampaignScenarioDto(
    UUID scenarioId,
    String scenarioName,
    String entryUrl,
    int weight
) {}
