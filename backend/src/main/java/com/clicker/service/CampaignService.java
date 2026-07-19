package com.clicker.service;

import com.clicker.domain.*;
import com.clicker.dto.*;
import com.clicker.repository.CampaignRepository;
import com.clicker.repository.CampaignRunRepository;
import com.clicker.repository.SiteRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CampaignService {

    private final CampaignRepository campaignRepository;
    private final SiteRepository siteRepository;
    private final CampaignRunRepository campaignRunRepository;
    private final SimulationEngine simulationEngine;
    private final ObjectMapper objectMapper;

    public CampaignService(CampaignRepository campaignRepository,
                           SiteRepository siteRepository,
                           CampaignRunRepository campaignRunRepository,
                           SimulationEngine simulationEngine,
                           ObjectMapper objectMapper) {
        this.campaignRepository = campaignRepository;
        this.siteRepository = siteRepository;
        this.campaignRunRepository = campaignRunRepository;
        this.simulationEngine = simulationEngine;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<CampaignDto> getUserCampaigns(UUID userId) {
        return campaignRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CampaignDto getCampaign(UUID id, UUID userId) {
        var campaign = campaignRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));
        if (!campaign.getUserId().equals(userId)) {
            throw new SecurityException("Access denied");
        }
        return toDto(campaign);
    }

    @Transactional
    public CampaignDto createCampaign(CampaignCreateRequest request, UUID userId) {
        var site = siteRepository.findById(request.siteId())
            .orElseThrow(() -> new IllegalArgumentException("Site not found"));
        if (!site.getUserId().equals(userId)) {
            throw new SecurityException("Access denied");
        }

        Campaign campaign;
        try {
            campaign = Campaign.builder()
                .userId(userId)
                .siteId(request.siteId())
                .name(request.name())
                .simulationLevel(request.simulationLevel() != null ? request.simulationLevel() : SimulationLevel.HTTP_ONLY)
                .trafficPattern(request.trafficPattern() != null ? request.trafficPattern() : TrafficPattern.CONSTANT)
                .visitsPerHour(request.visitsPerHour() > 0 ? request.visitsPerHour() : 100)
                .durationMinutes(request.durationMinutes() > 0 ? request.durationMinutes() : 60)
                .scheduleCron(request.scheduleCron())
                .geoDistribution(request.geoDistribution() != null
                    ? objectMapper.writeValueAsString(request.geoDistribution()) : "[]")
                .deviceProfile(request.deviceProfile() != null
                    ? objectMapper.writeValueAsString(request.deviceProfile()) : "[]")
                .userAgentConfig(request.userAgentConfig() != null
                    ? objectMapper.writeValueAsString(request.userAgentConfig())
                    : "{\"rotation\": \"RANDOM\"}")
                .proxyConfig(request.proxyConfig() != null
                    ? objectMapper.writeValueAsString(request.proxyConfig())
                    : "{\"provider\": \"ASOCKS\"}")
                .clickTargets(request.clickTargets() != null
                    ? objectMapper.writeValueAsString(request.clickTargets()) : "[]")
                .status(CampaignStatus.DRAFT)
                .build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create campaign", e);
        }

        campaign = campaignRepository.save(campaign);
        return toDto(campaign);
    }

    @Transactional
    public CampaignDto updateCampaign(UUID id, CampaignCreateRequest request, UUID userId) {
        var campaign = campaignRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));
        if (!campaign.getUserId().equals(userId)) {
            throw new SecurityException("Access denied");
        }
        if (campaign.getStatus() == CampaignStatus.RUNNING) {
            throw new IllegalStateException("Cannot update a running campaign");
        }

        try {
            campaign.setName(request.name());
            campaign.setSimulationLevel(request.simulationLevel());
            campaign.setTrafficPattern(request.trafficPattern());
            campaign.setVisitsPerHour(request.visitsPerHour());
            campaign.setDurationMinutes(request.durationMinutes());
            campaign.setScheduleCron(request.scheduleCron());
            campaign.setGeoDistribution(request.geoDistribution() != null
                ? objectMapper.writeValueAsString(request.geoDistribution()) : "[]");
            campaign.setDeviceProfile(request.deviceProfile() != null
                ? objectMapper.writeValueAsString(request.deviceProfile()) : "[]");
            campaign.setUserAgentConfig(request.userAgentConfig() != null
                ? objectMapper.writeValueAsString(request.userAgentConfig()) : "{\"rotation\": \"RANDOM\"}");
            campaign.setProxyConfig(request.proxyConfig() != null
                ? objectMapper.writeValueAsString(request.proxyConfig()) : "{\"provider\": \"ASOCKS\"}");
            campaign.setClickTargets(request.clickTargets() != null
                ? objectMapper.writeValueAsString(request.clickTargets()) : "[]");
            campaign.setSiteId(request.siteId());
        } catch (Exception e) {
            throw new RuntimeException("Failed to update campaign", e);
        }

        campaign = campaignRepository.save(campaign);
        return toDto(campaign);
    }

    @Transactional
    public void deleteCampaign(UUID id, UUID userId) {
        var campaign = campaignRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));
        if (!campaign.getUserId().equals(userId)) {
            throw new SecurityException("Access denied");
        }
        if (campaign.getStatus() == CampaignStatus.RUNNING) {
            simulationEngine.stopCampaign(id);
        }
        campaignRepository.delete(campaign);
    }

    @Transactional
    public CampaignRun startCampaign(UUID id, UUID userId) {
        var campaign = campaignRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));
        if (!campaign.getUserId().equals(userId)) {
            throw new SecurityException("Access denied");
        }
        return simulationEngine.startCampaign(id);
    }

    @Transactional
    public CampaignRun stopCampaign(UUID id, UUID userId) {
        var campaign = campaignRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));
        if (!campaign.getUserId().equals(userId)) {
            throw new SecurityException("Access denied");
        }
        return simulationEngine.stopCampaign(id);
    }

    @Transactional(readOnly = true)
    public List<CampaignRunDto> getRuns(UUID campaignId, UUID userId) {
        var campaign = campaignRepository.findById(campaignId)
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));
        if (!campaign.getUserId().equals(userId)) {
            throw new SecurityException("Access denied");
        }
        return campaignRunRepository.findByCampaignIdOrderByStartedAtDesc(campaignId)
            .stream()
            .map(r -> new CampaignRunDto(
                r.getId(), r.getCampaignId(), r.getStatus(),
                r.getStartedAt(), r.getFinishedAt(),
                r.getTotalVisits(), r.getSuccessfulVisits(), r.getFailedVisits(),
                r.getStats()
            ))
            .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SimulationStats getLiveStats(UUID id, UUID userId) {
        var campaign = campaignRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));
        if (!campaign.getUserId().equals(userId)) {
            throw new SecurityException("Access denied");
        }
        return simulationEngine.getStats(id);
    }

    @Transactional
    public void pauseCampaign(UUID id, UUID userId) {
        var campaign = campaignRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));
        if (!campaign.getUserId().equals(userId)) {
            throw new SecurityException("Access denied");
        }
        simulationEngine.pauseCampaign(id);
    }

    @Transactional
    public void resumeCampaign(UUID id, UUID userId) {
        var campaign = campaignRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));
        if (!campaign.getUserId().equals(userId)) {
            throw new SecurityException("Access denied");
        }
        simulationEngine.resumeCampaign(id);
    }

    private CampaignDto toDto(Campaign c) {
        List<GeoDistributionDto> geos = new ArrayList<>();
        List<DeviceProfileDto> devices = new ArrayList<>();
        List<ClickTargetDto> clickTargets = new ArrayList<>();
        UserAgentConfigDto uaConfig = new UserAgentConfigDto("RANDOM", List.of());
        ProxyConfigDto proxyConfig = new ProxyConfigDto("ASOCKS");

        try {
            geos = objectMapper.readValue(c.getGeoDistribution(), new TypeReference<List<GeoDistributionDto>>() {});
        } catch (Exception ignored) {}
        try {
            devices = objectMapper.readValue(c.getDeviceProfile(), new TypeReference<List<DeviceProfileDto>>() {});
        } catch (Exception ignored) {}
        try {
            uaConfig = objectMapper.readValue(c.getUserAgentConfig(), UserAgentConfigDto.class);
        } catch (Exception ignored) {}
        try {
            proxyConfig = objectMapper.readValue(c.getProxyConfig(), ProxyConfigDto.class);
        } catch (Exception ignored) {}
        try {
            clickTargets = objectMapper.readValue(c.getClickTargets(), new TypeReference<List<ClickTargetDto>>() {});
        } catch (Exception ignored) {}

        List<CampaignScenarioDto> scenarios = c.getCampaignScenarios() != null
            ? c.getCampaignScenarios().stream()
                .map(cs -> new CampaignScenarioDto(
                    cs.getScenarioId(),
                    cs.getScenario() != null ? cs.getScenario().getName() : "",
                    cs.getEntryUrl(),
                    cs.getWeight()
                ))
                .collect(Collectors.toList())
            : List.of();

        Site site = siteRepository.findById(c.getSiteId()).orElse(null);
        String siteName = site != null ? site.getName() : "";
        String siteBaseUrl = site != null ? site.getBaseUrl() : "";

        return new CampaignDto(
            c.getId(), c.getUserId(), c.getSiteId(), siteName, siteBaseUrl,
            c.getName(), c.getStatus(), c.getSimulationLevel(),
            c.getTrafficPattern(), c.getVisitsPerHour(), c.getDurationMinutes(),
            c.getScheduleCron(), geos, devices, uaConfig, proxyConfig,
            clickTargets, scenarios, c.getCreatedAt(), c.getUpdatedAt(), c.getLastRunAt()
        );
    }
}
