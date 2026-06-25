package com.clicker.service;

import com.clicker.domain.*;
import com.clicker.dto.GeoDistributionDto;
import com.clicker.dto.SimulationStats;
import com.clicker.repository.CampaignRepository;
import com.clicker.repository.CampaignRunRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class SimulationEngine {

    private static final Logger log = LoggerFactory.getLogger(SimulationEngine.class);

    private final CampaignRepository campaignRepository;
    private final CampaignRunRepository campaignRunRepository;
    private final AsocksService asocksService;
    private final UserAgentService userAgentService;
    private final WebSocketPublisher webSocketPublisher;
    private final ObjectMapper objectMapper;
    private final HttpSimulationWorker httpWorker;
    private final BrowserSimulationWorker browserWorker;

    private final Map<UUID, SimulationContext> activeSimulations = new ConcurrentHashMap<>();
    private final ScheduledExecutorService globalScheduler = Executors.newScheduledThreadPool(4, Thread.ofVirtual().factory());

    public SimulationEngine(CampaignRepository campaignRepository,
                            CampaignRunRepository campaignRunRepository,
                            AsocksService asocksService,
                            UserAgentService userAgentService,
                            WebSocketPublisher webSocketPublisher,
                            ObjectMapper objectMapper,
                            HttpSimulationWorker httpWorker,
                            BrowserSimulationWorker browserWorker) {
        this.campaignRepository = campaignRepository;
        this.campaignRunRepository = campaignRunRepository;
        this.asocksService = asocksService;
        this.userAgentService = userAgentService;
        this.webSocketPublisher = webSocketPublisher;
        this.objectMapper = objectMapper;
        this.httpWorker = httpWorker;
        this.browserWorker = browserWorker;
    }

    public record SimulationContext(
        UUID campaignId,
        UUID runId,
        Campaign campaign,
        CampaignRun run,
        ScheduledExecutorService scheduler,
        AtomicBoolean running,
        AtomicInteger totalVisits,
        AtomicInteger successfulVisits,
        AtomicInteger failedVisits,
        Instant startTime,
        ScheduledFuture<?> stopFuture
    ) {}

    @Transactional
    public CampaignRun startCampaign(UUID campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + campaignId));

        if (activeSimulations.containsKey(campaignId)) {
            throw new IllegalStateException("Campaign is already running");
        }

        campaign.setStatus(CampaignStatus.RUNNING);
        campaignRepository.save(campaign);

        CampaignRun run = CampaignRun.builder()
            .campaignId(campaignId)
            .status(RunStatus.RUNNING)
            .build();
        run = campaignRunRepository.save(run);

        String poolKey = "run-" + run.getId();
        Map<String, Integer> geoMap = buildGeoCountMap(campaign);
        asocksService.initPool(poolKey, geoMap);

        var context = new SimulationContext(
            campaignId, run.getId(), campaign, run,
            Executors.newScheduledThreadPool(2, Thread.ofVirtual().factory()),
            new AtomicBoolean(true),
            new AtomicInteger(0),
            new AtomicInteger(0),
            new AtomicInteger(0),
            Instant.now(),
            null
        );

        long intervalMs = calculateIntervalMs(campaign);
        SimulationLevel level = campaign.getSimulationLevel();
        int durationMinutes = campaign.getDurationMinutes();

        ScheduledFuture<?> visitTask = context.scheduler().scheduleAtFixedRate(() -> {
            if (!context.running().get()) return;
            executeVisit(context, poolKey, level);
        }, 0, intervalMs, TimeUnit.MILLISECONDS);

        ScheduledFuture<?> stopFuture = globalScheduler.schedule(() -> {
            log.info("Duration reached for campaign {}, stopping", campaignId);
            stopCampaign(campaignId);
        }, durationMinutes, TimeUnit.MINUTES);

        var finalContext = new SimulationContext(
            context.campaignId(), context.runId(), context.campaign(), context.run(),
            context.scheduler(), context.running(),
            context.totalVisits(), context.successfulVisits(), context.failedVisits(),
            context.startTime(), stopFuture
        );

        activeSimulations.put(campaignId, finalContext);

        webSocketPublisher.sendStatus(run.getId().toString(), "RUNNING");
        log.info("Started campaign '{}' — {} visits/h, {}m duration, {} simulation",
            campaign.getName(), campaign.getVisitsPerHour(), durationMinutes, level);

        return run;
    }

    private void executeVisit(SimulationContext context, String poolKey, SimulationLevel level) {
        GeoDistributionDto geo = pickGeo(context.campaign());
        String countryCode = geo != null ? geo.countryCode() : "US";

        String proxy = asocksService.acquireProxy(poolKey, countryCode);
        if (proxy == null) {
            log.warn("No proxy available for {} in pool '{}'", countryCode, poolKey);
            return;
        }

        boolean success = false;
        try {
            switch (level) {
                case HTTP_ONLY -> success = httpWorker.visitSimple(
                    context.campaign(), proxy, countryCode, context);
                case BROWSER_NAVIGATION -> success = httpWorker.visitWithNavigation(
                    context.campaign(), proxy, countryCode, context);
                case FULL_BROWSER -> success = browserWorker.visitFull(
                    context.campaign(), proxy, countryCode, context);
            }

            context.totalVisits().incrementAndGet();
            if (success) {
                context.successfulVisits().incrementAndGet();
            } else {
                context.failedVisits().incrementAndGet();
            }

            updateRunStats(context);
        } catch (Exception e) {
            log.error("Visit execution failed", e);
            context.totalVisits().incrementAndGet();
            context.failedVisits().incrementAndGet();
            updateRunStats(context);
        }
    }

    private GeoDistributionDto pickGeo(Campaign campaign) {
        try {
            var geos = objectMapper.readValue(
                campaign.getGeoDistribution(),
                new TypeReference<List<GeoDistributionDto>>() {}
            );
            if (geos.isEmpty()) return new GeoDistributionDto("US", "US", null, 100);

            int totalWeight = geos.stream().mapToInt(GeoDistributionDto::weight).sum();
            if (totalWeight <= 0) return geos.get(0);

            int roll = ThreadLocalRandom.current().nextInt(totalWeight);
            int cumulative = 0;
            for (GeoDistributionDto geo : geos) {
                cumulative += geo.weight();
                if (roll < cumulative) return geo;
            }
            return geos.get(geos.size() - 1);
        } catch (Exception e) {
            return new GeoDistributionDto("US", "US", null, 100);
        }
    }

    @Transactional
    public CampaignRun stopCampaign(UUID campaignId) {
        var context = activeSimulations.remove(campaignId);
        if (context == null) return null;

        context.running().set(false);
        context.scheduler().shutdownNow();
        if (context.stopFuture() != null) {
            context.stopFuture().cancel(false);
        }

        Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
        if (campaign != null) {
            campaign.setStatus(CampaignStatus.COMPLETED);
            campaign.setLastRunAt(Instant.now());
            campaignRepository.save(campaign);
        }

        CampaignRun run = campaignRunRepository.findById(context.runId()).orElse(null);
        if (run != null) {
            run.setStatus(RunStatus.COMPLETED);
            run.setFinishedAt(Instant.now());
            run.setTotalVisits(context.totalVisits().get());
            run.setSuccessfulVisits(context.successfulVisits().get());
            run.setFailedVisits(context.failedVisits().get());
            run = campaignRunRepository.save(run);
        }

        asocksService.cleanupPool("run-" + context.runId());

        webSocketPublisher.sendStatus(context.runId().toString(), "COMPLETED");
        log.info("Stopped campaign '{}' — {} total, {} ok, {} failed",
            campaign != null ? campaign.getName() : campaignId,
            context.totalVisits().get(), context.successfulVisits().get(), context.failedVisits().get());

        return run;
    }

    @Transactional
    public void pauseCampaign(UUID campaignId) {
        var context = activeSimulations.get(campaignId);
        if (context == null) throw new IllegalStateException("Campaign is not running");
        context.running().set(false);
        if (context.stopFuture() != null) context.stopFuture().cancel(false);
        Campaign campaign = context.campaign();
        campaign.setStatus(CampaignStatus.PAUSED);
        campaignRepository.save(campaign);
        webSocketPublisher.sendStatus(context.runId().toString(), "PAUSED");
    }

    @Transactional
    public void resumeCampaign(UUID campaignId) {
        var context = activeSimulations.get(campaignId);
        if (context == null) throw new IllegalStateException("Campaign is not paused");
        context.running().set(true);
        Campaign campaign = context.campaign();
        campaign.setStatus(CampaignStatus.RUNNING);
        campaignRepository.save(campaign);

        int remainingMinutes = campaign.getDurationMinutes();
        if (context.stopFuture() != null) context.stopFuture().cancel(false);
        ScheduledFuture<?> stopFuture = globalScheduler.schedule(() -> stopCampaign(campaignId),
            remainingMinutes, TimeUnit.MINUTES);

        var newContext = new SimulationContext(
            context.campaignId(), context.runId(), context.campaign(), context.run(),
            context.scheduler(), context.running(),
            context.totalVisits(), context.successfulVisits(), context.failedVisits(),
            context.startTime(), stopFuture
        );
        activeSimulations.put(campaignId, newContext);

        webSocketPublisher.sendStatus(context.runId().toString(), "RUNNING");
    }

    public SimulationStats getStats(UUID campaignId) {
        var context = activeSimulations.get(campaignId);
        if (context == null) return null;

        Duration elapsed = Duration.between(context.startTime(), Instant.now());
        long seconds = elapsed.getSeconds();
        double vps = seconds > 0 ? (double) context.totalVisits().get() / seconds : 0;
        int available = asocksService.availableProxies("run-" + context.runId());

        return new SimulationStats(
            campaignId.toString(),
            context.runId().toString(),
            context.totalVisits().get(),
            context.successfulVisits().get(),
            context.failedVisits().get(),
            available,
            Math.round(vps * 100.0) / 100.0,
            formatDuration(elapsed),
            "N/A"
        );
    }

    public boolean isRunning(UUID campaignId) {
        var ctx = activeSimulations.get(campaignId);
        return ctx != null && ctx.running().get();
    }

    public void shutdown() {
        log.info("Shutting down simulation engine...");
        new ArrayList<>(activeSimulations.keySet()).forEach(id -> {
            try {
                stopCampaign(id);
            } catch (Exception e) {
                log.error("Failed to stop campaign {} during shutdown", id, e);
            }
        });
        globalScheduler.shutdownNow();
    }

    private void updateRunStats(SimulationContext context) {
        try {
            CampaignRun run = campaignRunRepository.findById(context.runId()).orElse(null);
            if (run != null) {
                run.setTotalVisits(context.totalVisits().get());
                run.setSuccessfulVisits(context.successfulVisits().get());
                run.setFailedVisits(context.failedVisits().get());
                campaignRunRepository.save(run);
            }
            webSocketPublisher.sendStats(context.runId().toString(), getStats(context.campaignId()));
        } catch (Exception e) {
            log.error("Failed to update run stats", e);
        }
    }

    private Map<String, Integer> buildGeoCountMap(Campaign campaign) {
        try {
            var geos = objectMapper.readValue(
                campaign.getGeoDistribution(),
                new TypeReference<List<GeoDistributionDto>>() {}
            );
            int portCount = Math.min(Math.max(campaign.getVisitsPerHour() / 60, 5), 20);
            Map<String, Integer> result = new LinkedHashMap<>();

            int totalWeight = geos.stream().mapToInt(GeoDistributionDto::weight).sum();
            if (totalWeight == 0) totalWeight = 100;

            for (var geo : geos) {
                int count = Math.max(1, portCount * geo.weight() / totalWeight);
                result.put(geo.countryCode(), count);
            }

            if (result.isEmpty()) result.put("US", portCount);
            return result;
        } catch (Exception e) {
            return Map.of("US", 5);
        }
    }

    private long calculateIntervalMs(Campaign campaign) {
        double visitsPerSecond = campaign.getVisitsPerHour() / 3600.0;
        if (visitsPerSecond <= 0) visitsPerSecond = 0.1;
        return (long) (1000.0 / visitsPerSecond);
    }

    private String formatDuration(Duration d) {
        return String.format("%02d:%02d:%02d", d.toHours(), d.toMinutesPart(), d.toSecondsPart());
    }
}
