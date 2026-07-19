package com.clicker.dto;

import com.clicker.service.BrowserSimulationWorker.DiscoveredElement;
import com.clicker.dto.NavigationStepDto;
import java.util.List;

public record SessionState(
    String sessionId,
    String screenshot,
    List<DiscoveredElement> elements,
    String currentUrl,
    List<NavigationStepDto> currentPath,
    int pageWidth,
    int pageHeight
) {}
