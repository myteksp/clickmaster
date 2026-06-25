package com.clicker.controller;

import com.clicker.dto.CampaignCreateRequest;
import com.clicker.dto.CampaignDto;
import com.clicker.dto.CampaignRunDto;
import com.clicker.dto.SimulationStats;
import com.clicker.service.CampaignService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/campaigns")
public class CampaignController {

    private final CampaignService campaignService;

    public CampaignController(CampaignService campaignService) {
        this.campaignService = campaignService;
    }

    private UUID toUuid(String s) {
        return UUID.fromString(s);
    }

    @GetMapping
    public ResponseEntity<List<CampaignDto>> list(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(campaignService.getUserCampaigns(toUuid(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CampaignDto> get(@PathVariable UUID id,
                                           @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(campaignService.getCampaign(id, toUuid(userId)));
    }

    @PostMapping
    public ResponseEntity<CampaignDto> create(@Valid @RequestBody CampaignCreateRequest request,
                                              @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(campaignService.createCampaign(request, toUuid(userId)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CampaignDto> update(@PathVariable UUID id,
                                              @Valid @RequestBody CampaignCreateRequest request,
                                              @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(campaignService.updateCampaign(id, request, toUuid(userId)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id,
                                       @AuthenticationPrincipal String userId) {
        campaignService.deleteCampaign(id, toUuid(userId));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<CampaignRunDto> start(@PathVariable UUID id,
                                                @AuthenticationPrincipal String userId) {
        var run = campaignService.startCampaign(id, toUuid(userId));
        return ResponseEntity.ok(new CampaignRunDto(
            run.getId(), run.getCampaignId(), run.getStatus(),
            run.getStartedAt(), run.getFinishedAt(),
            run.getTotalVisits(), run.getSuccessfulVisits(), run.getFailedVisits(),
            run.getStats()
        ));
    }

    @PostMapping("/{id}/stop")
    public ResponseEntity<CampaignRunDto> stop(@PathVariable UUID id,
                                               @AuthenticationPrincipal String userId) {
        var run = campaignService.stopCampaign(id, toUuid(userId));
        return ResponseEntity.ok(new CampaignRunDto(
            run.getId(), run.getCampaignId(), run.getStatus(),
            run.getStartedAt(), run.getFinishedAt(),
            run.getTotalVisits(), run.getSuccessfulVisits(), run.getFailedVisits(),
            run.getStats()
        ));
    }

    @PostMapping("/{id}/pause")
    public ResponseEntity<Void> pause(@PathVariable UUID id,
                                      @AuthenticationPrincipal String userId) {
        campaignService.pauseCampaign(id, toUuid(userId));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/resume")
    public ResponseEntity<Void> resume(@PathVariable UUID id,
                                       @AuthenticationPrincipal String userId) {
        campaignService.resumeCampaign(id, toUuid(userId));
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}/runs")
    public ResponseEntity<List<CampaignRunDto>> runs(@PathVariable UUID id,
                                                     @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(campaignService.getRuns(id, toUuid(userId)));
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<SimulationStats> stats(@PathVariable UUID id,
                                                 @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(campaignService.getLiveStats(id, toUuid(userId)));
    }
}
