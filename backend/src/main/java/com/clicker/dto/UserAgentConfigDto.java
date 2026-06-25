package com.clicker.dto;

import java.util.List;

public record UserAgentConfigDto(
    String rotation,
    List<String> customPool
) {}
