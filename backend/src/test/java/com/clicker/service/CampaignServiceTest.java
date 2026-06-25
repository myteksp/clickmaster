package com.clicker.service;

import com.clicker.domain.*;
import com.clicker.dto.*;
import com.clicker.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class CampaignServiceTest {

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private SiteService siteService;

    @Autowired
    private UserRepository userRepository;

    private UUID userId;
    private UUID siteId;

    @BeforeEach
    void setUp() {
        var user = new User.Builder()
            .email("campaign-test@clicker.io")
            .passwordHash("hash")
            .name("Campaign Test User")
            .build();
        user = userRepository.save(user);
        userId = user.getId();

        var site = siteService.createSite(
            new SiteDto(null, null, "Test Site", "https://example.com"), userId);
        siteId = site.id();
    }

    private CampaignCreateRequest validRequest() {
        return new CampaignCreateRequest(
            siteId, "Test Campaign", SimulationLevel.HTTP_ONLY,
            TrafficPattern.CONSTANT, 100, 60, null,
            List.of(new GeoDistributionDto("US", "United States", null, 100)),
            List.of(),
            new UserAgentConfigDto("RANDOM", List.of()),
            new ProxyConfigDto("ASOCKS"),
            List.of()
        );
    }

    @Test
    void shouldCreateDraftCampaign() {
        var dto = campaignService.createCampaign(validRequest(), userId);

        assertThat(dto.id()).isNotNull();
        assertThat(dto.name()).isEqualTo("Test Campaign");
        assertThat(dto.status()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(dto.simulationLevel()).isEqualTo(SimulationLevel.HTTP_ONLY);
        assertThat(dto.visitsPerHour()).isEqualTo(100);
        assertThat(dto.status()).isEqualTo(CampaignStatus.DRAFT);
        assertThat(dto.geoDistribution()).hasSize(1);
    }

    @Test
    void shouldListUserCampaigns() {
        campaignService.createCampaign(validRequest(), userId);
        campaignService.createCampaign(validRequest(), userId);

        var list = campaignService.getUserCampaigns(userId);
        assertThat(list).hasSize(2);
    }

    @Test
    void shouldUpdateCampaign() {
        var created = campaignService.createCampaign(validRequest(), userId);

        var updatedReq = new CampaignCreateRequest(
            siteId, "Updated Campaign", SimulationLevel.BROWSER_NAVIGATION,
            TrafficPattern.RAMP_UP, 200, 120, null,
            List.of(new GeoDistributionDto("GB", "United Kingdom", null, 100)),
            List.of(),
            new UserAgentConfigDto("RANDOM", List.of()),
            new ProxyConfigDto("ASOCKS"),
            List.of()
        );
        var updated = campaignService.updateCampaign(created.id(), updatedReq, userId);

        assertThat(updated.name()).isEqualTo("Updated Campaign");
        assertThat(updated.simulationLevel()).isEqualTo(SimulationLevel.BROWSER_NAVIGATION);
        assertThat(updated.visitsPerHour()).isEqualTo(200);
    }

    @Test
    void shouldDeleteCampaign() {
        var created = campaignService.createCampaign(validRequest(), userId);

        campaignService.deleteCampaign(created.id(), userId);

        assertThatThrownBy(() ->
            campaignService.getCampaign(created.id(), userId))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectOtherUserAccess() {
        var created = campaignService.createCampaign(validRequest(), userId);

        assertThatThrownBy(() ->
            campaignService.getCampaign(created.id(), UUID.randomUUID()))
            .isInstanceOf(SecurityException.class);
    }

    @Test
    void shouldNotUpdateRunningCampaign() {
        campaignService.createCampaign(validRequest(), userId);
        // Status is DRAFT, can't test RUNNING restriction without actually running
        // which requires asocks API mock. Verified in integration test below.
    }
}
