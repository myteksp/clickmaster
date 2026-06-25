package com.clicker.dto;

public record DeviceProfileDto(
    String device,
    String os,
    String browser,
    int weight
) {}
