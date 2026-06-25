package com.clicker.dto;

import java.util.List;
import java.util.UUID;

public record ScenarioDto(
    UUID id,
    UUID userId,
    String name,
    String description,
    List<StepDto> steps
) {}
