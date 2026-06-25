package com.clicker.controller;

import com.clicker.dto.SiteDto;
import com.clicker.service.SiteService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/sites")
public class SiteController {

    private final SiteService siteService;

    public SiteController(SiteService siteService) {
        this.siteService = siteService;
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
}
