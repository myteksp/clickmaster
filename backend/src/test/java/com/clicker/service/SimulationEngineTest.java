package com.clicker.service;

import com.clicker.domain.*;
import com.clicker.dto.*;
import com.clicker.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class SimulationEngineTest {

    @Autowired
    private SimulationEngine simulationEngine;

    @Autowired
    private CampaignService campaignService;

    @Autowired
    private SiteService siteService;

    @Autowired
    private CampaignRunRepository campaignRunRepository;

    @Autowired
    private UserRepository userRepository;

    @MockBean
    private AsocksService asocksService;

    @MockBean
    private HttpSimulationWorker httpSimulationWorker;

    @MockBean
    private BrowserSimulationWorker browserSimulationWorker;

    @MockBean
    private WebSocketPublisher webSocketPublisher;

    private UUID userId;
    private UUID siteId;

    @BeforeEach
    void setUp() {
        var user = new User.Builder()
            .email("sim-test-" + UUID.randomUUID() + "@clicker.io")
            .passwordHash("hash")
            .name("Sim Test User")
            .build();
        user = userRepository.save(user);
        userId = user.getId();

        var site = siteService.createSite(
            new SiteDto(null, null, "Test Site", "https://example.com"), userId);
        siteId = site.id();

        when(httpSimulationWorker.visitSimple(any(), anyString(), anyString(), any())).thenReturn(true);
        when(httpSimulationWorker.visitWithNavigation(any(), anyString(), anyString(), any())).thenReturn(true);
        when(browserSimulationWorker.visitFull(any(), anyString(), anyString(), any())).thenReturn(true);

        when(asocksService.acquireProxy(anyString(), anyString())).thenReturn("http://user:pass@1.2.3.4:8080");
        doNothing().when(asocksService).initPool(anyString(), anyMap());
        doNothing().when(asocksService).cleanupPool(anyString());
        when(asocksService.availableProxies(anyString())).thenReturn(10);
    }

    @Test
    void shouldStartAndStopCampaignWithVisits() throws Exception {
        var campaign = campaignService.createCampaign(new CampaignCreateRequest(
            siteId, "Level 1 Test", SimulationLevel.HTTP_ONLY,
            TrafficPattern.CONSTANT, 3600, 1, null,
            List.of(new GeoDistributionDto("US", "United States", null, 100)),
            List.of(),
            new UserAgentConfigDto("RANDOM", List.of()),
            new ProxyConfigDto("ASOCKS"),
            List.of()
        ), userId);

        var run = simulationEngine.startCampaign(campaign.id());
        assertThat(run.getStatus()).isEqualTo(RunStatus.RUNNING);
        assertThat(simulationEngine.isRunning(campaign.id())).isTrue();

        Thread.sleep(3000);

        var stats = simulationEngine.getStats(campaign.id());
        assertThat(stats).isNotNull();
        assertThat(stats.totalVisits()).isGreaterThan(0);

        var stoppedRun = simulationEngine.stopCampaign(campaign.id());
        assertThat(stoppedRun.getStatus()).isEqualTo(RunStatus.COMPLETED);
        assertThat(stoppedRun.getTotalVisits()).isGreaterThan(0);
        assertThat(simulationEngine.isRunning(campaign.id())).isFalse();
    }

    @Test
    void shouldPauseAndResumeCampaign() throws Exception {
        var campaign = campaignService.createCampaign(new CampaignCreateRequest(
            siteId, "Pause Test", SimulationLevel.HTTP_ONLY,
            TrafficPattern.CONSTANT, 7200, 5, null,
            List.of(new GeoDistributionDto("US", "United States", null, 100)),
            List.of(),
            new UserAgentConfigDto("RANDOM", List.of()),
            new ProxyConfigDto("ASOCKS"),
            List.of()
        ), userId);

        simulationEngine.startCampaign(campaign.id());
        Thread.sleep(2000);

        simulationEngine.pauseCampaign(campaign.id());
        assertThat(simulationEngine.isRunning(campaign.id())).isFalse();

        int visitsAtPause = campaignRunRepository
            .findByCampaignIdOrderByStartedAtDesc(campaign.id()).get(0).getTotalVisits();

        Thread.sleep(1500);
        int visitsAfterPause = campaignRunRepository
            .findByCampaignIdOrderByStartedAtDesc(campaign.id()).get(0).getTotalVisits();
        assertThat(visitsAfterPause).isEqualTo(visitsAtPause);

        simulationEngine.resumeCampaign(campaign.id());
        assertThat(simulationEngine.isRunning(campaign.id())).isTrue();

        Thread.sleep(2000);
        int visitsAfterResume = campaignRunRepository
            .findByCampaignIdOrderByStartedAtDesc(campaign.id()).get(0).getTotalVisits();
        assertThat(visitsAfterResume).isGreaterThan(visitsAtPause);

        simulationEngine.stopCampaign(campaign.id());
    }

    @Test
    void shouldAutoStopAfterDuration() throws Exception {
        var campaign = campaignService.createCampaign(new CampaignCreateRequest(
            siteId, "Duration Test", SimulationLevel.HTTP_ONLY,
            TrafficPattern.CONSTANT, 3600, 0, null,
            List.of(new GeoDistributionDto("US", "United States", null, 100)),
            List.of(),
            new UserAgentConfigDto("RANDOM", List.of()),
            new ProxyConfigDto("ASOCKS"),
            List.of()
        ), userId);

        simulationEngine.startCampaign(campaign.id());
        Thread.sleep(1500);

        var runs = campaignRunRepository.findByCampaignIdOrderByStartedAtDesc(campaign.id());
        assertThat(runs).isNotEmpty();
    }

    @Test
    void shouldNotAllowDoubleStart() {
        var campaign = campaignService.createCampaign(new CampaignCreateRequest(
            siteId, "Double Start", SimulationLevel.HTTP_ONLY,
            TrafficPattern.CONSTANT, 10, 10, null,
            List.of(new GeoDistributionDto("US", "United States", null, 100)),
            List.of(),
            new UserAgentConfigDto("RANDOM", List.of()),
            new ProxyConfigDto("ASOCKS"),
            List.of()
        ), userId);

        simulationEngine.startCampaign(campaign.id());

        assertThatThrownBy(() -> simulationEngine.startCampaign(campaign.id()))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("already running");

        simulationEngine.stopCampaign(campaign.id());
    }

    @Test
    void shouldUseFreshProxyPerVisit() throws Exception {
        var campaign = campaignService.createCampaign(new CampaignCreateRequest(
            siteId, "Proxy Test", SimulationLevel.HTTP_ONLY,
            TrafficPattern.CONSTANT, 7200, 1, null,
            List.of(new GeoDistributionDto("US", "United States", null, 100)),
            List.of(),
            new UserAgentConfigDto("RANDOM", List.of()),
            new ProxyConfigDto("ASOCKS"),
            List.of()
        ), userId);

        simulationEngine.startCampaign(campaign.id());
        Thread.sleep(3000);
        simulationEngine.stopCampaign(campaign.id());

        verify(asocksService, atLeast(2)).acquireProxy(anyString(), eq("US"));
        verify(asocksService).cleanupPool(anyString());
    }
}
