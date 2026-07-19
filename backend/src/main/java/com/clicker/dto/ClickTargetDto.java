package com.clicker.dto;

import java.util.List;

public record ClickTargetDto(
    String selector,
    String text,
    String tag,
    String pagePath,
    List<NavigationStepDto> navigationSteps,
    int probability,
    int delayBeforeMs,
    int delayAfterMs
) {
    public ClickTargetDto {
        if (navigationSteps == null) navigationSteps = List.of();
    }
}
