package com.clicker.dto;

import com.clicker.domain.StepAction;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StepDto(
    UUID id,
    int orderIndex,
    @NotNull StepAction actionType,
    String selector,
    String value,
    int delayBeforeMs,
    int delayAfterMs,
    double probability,
    String config
) {}
