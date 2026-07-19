package com.clicker.controller;

import com.clicker.dto.SessionState;
import com.clicker.dto.SiteDto;
import com.clicker.repository.SiteRepository;
import com.clicker.service.BrowserSimulationWorker;
import com.clicker.service.SiteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/sites")
public class SiteController {

    private final SiteService siteService;
    private final SiteRepository siteRepository;
    private final BrowserSimulationWorker browserWorker;

    public SiteController(SiteService siteService, SiteRepository siteRepository,
                          BrowserSimulationWorker browserWorker) {
        this.siteService = siteService;
        this.siteRepository = siteRepository;
        this.browserWorker = browserWorker;
    }

    @GetMapping
    public ResponseEntity<List<SiteDto>> list(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(siteService.getUserSites(UUID.fromString(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SiteDto> get(@PathVariable UUID id,
                                       @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(siteService.getSite(id, UUID.fromString(userId)));
    }

    @PostMapping
    public ResponseEntity<SiteDto> create(@Valid @RequestBody SiteDto dto,
                                          @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(siteService.createSite(dto, UUID.fromString(userId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SiteDto> update(@PathVariable UUID id,
                                          @Valid @RequestBody SiteDto dto,
                                          @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(siteService.updateSite(id, dto, UUID.fromString(userId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                       @AuthenticationPrincipal String userId) {
        siteService.deleteSite(id, UUID.fromString(userId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/discover-elements")
    public ResponseEntity<BrowserSimulationWorker.SitePreview> discoverElements(
            @PathVariable UUID id,
            @AuthenticationPrincipal String userId) {
        var site = siteRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Site not found"));
        if (!site.getUserId().equals(UUID.fromString(userId))) {
            throw new SecurityException("Access denied");
        }
        return ResponseEntity.ok(browserWorker.discoverElements(site.getBaseUrl()));
    }

    @PostMapping("/{id}/session/start")
    public ResponseEntity<SessionState> startSession(
            @PathVariable UUID id,
            @AuthenticationPrincipal String userId) {
        var site = siteRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Site not found"));
        if (!site.getUserId().equals(UUID.fromString(userId))) {
            throw new SecurityException("Access denied");
        }
        return ResponseEntity.ok(browserWorker.startSession(site.getBaseUrl()));
    }

    @PostMapping("/{id}/session/{sessionId}/click")
    public ResponseEntity<SessionState> sessionClick(
            @PathVariable UUID id,
            @PathVariable String sessionId,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(browserWorker.sessionClick(
            sessionId, body.get("selector"), body.getOrDefault("text", "")));
    }

    @PostMapping("/{id}/session/{sessionId}/back")
    public ResponseEntity<SessionState> sessionBack(
            @PathVariable UUID id,
            @PathVariable String sessionId,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(browserWorker.sessionBack(sessionId));
    }

    @DeleteMapping("/{id}/session/{sessionId}")
    public ResponseEntity<Void> closeSession(
            @PathVariable UUID id,
            @PathVariable String sessionId,
            @AuthenticationPrincipal String userId) {
        browserWorker.closeSession(sessionId);
        return ResponseEntity.noContent().build();
    }
}
