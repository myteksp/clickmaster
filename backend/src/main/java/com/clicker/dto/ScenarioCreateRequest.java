package com.clicker.dto;

import com.clicker.domain.StepAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record ScenarioCreateRequest(
    @NotBlank String name,
    String description,
    List<StepDto> steps
) {}
