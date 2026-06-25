package com.clicker.dto;

import java.util.UUID;

public record CampaignScenarioLink(
    UUID scenarioId,
    String entryUrl,
    int weight
) {}
