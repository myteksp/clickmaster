package com.clicker.dto;

import java.util.List;
import java.util.UUID;

public record GeoDistributionDto(
    String countryCode,
    String countryName,
    String city,
    int weight
) {}
