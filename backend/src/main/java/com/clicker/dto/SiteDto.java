package com.clicker.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SiteDto(
    UUID id,
    UUID userId,
    @NotBlank @Size(max = 255) String name,
    @NotBlank @Size(max = 2048) String baseUrl
) {}
