package com.clicker.dto;

public record NavigationStepDto(
    String selector,
    String text,
    int waitAfterMs
) {}
