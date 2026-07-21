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
    private final WebSocketPublisher webSocketPublisher;
    private final ObjectMapper objectMapper;
    private final HttpSimulationWorker httpWorker;
    private final BrowserSimulationWorker browserWorker;

    private final ScheduledExecutorService globalScheduler = Executors.newScheduledThreadPool(4, Thread.ofVirtual().factory());

    public record SimulationContext(
        UUID campaignId,
        UUID runId,
        Campaign campaign,
        ExecutorService visitExecutor,
        AtomicBoolean running,
        AtomicInteger totalVisits,
        AtomicInteger successfulVisits,
        AtomicInteger failedVisits,
        Instant startTime,
        Semaphore concurrencyLimit
    ) {}

    private record ActiveRun(
        SimulationContext context,
        ScheduledFuture<?> stopFuture,
        Thread dispatchThread
    ) {}

    private final Map<UUID, ActiveRun> activeRuns = new ConcurrentHashMap<>();

    public SimulationEngine(CampaignRepository campaignRepository,
                            CampaignRunRepository campaignRunRepository,
                            AsocksService asocksService,
                            WebSocketPublisher webSocketPublisher,
                            ObjectMapper objectMapper,
                            HttpSimulationWorker httpWorker,
                            BrowserSimulationWorker browserWorker) {
        this.campaignRepository = campaignRepository;
        this.campaignRunRepository = campaignRunRepository;
        this.asocksService = asocksService;
        this.webSocketPublisher = webSocketPublisher;
        this.objectMapper = objectMapper;
        this.httpWorker = httpWorker;
        this.browserWorker = browserWorker;
    }

    @Transactional
    public CampaignRun startCampaign(UUID campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found: " + campaignId));

        if (campaign.getStatus() == CampaignStatus.RUNNING) {
            throw new IllegalStateException("Campaign is already running");
        }

        String ignored = campaign.getSite() != null ? campaign.getSite().getBaseUrl() : "";

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

        var ctx = new SimulationContext(
            campaignId, run.getId(), campaign,
            Executors.newVirtualThreadPerTaskExecutor(),
            new AtomicBoolean(true),
            new AtomicInteger(0), new AtomicInteger(0), new AtomicInteger(0),
            Instant.now(),
            new Semaphore(10)
        );

        long intervalMs = calculateIntervalMs(campaign);
        Thread dispatchThread = startDispatchLoop(ctx, poolKey, campaign.getSimulationLevel(), intervalMs);

        ScheduledFuture<?> stopFuture = globalScheduler.schedule(() -> {
            log.info("Duration reached for campaign {}, stopping", campaignId);
            try { stopCampaign(campaignId); } catch (Exception e) {
                log.error("Auto-stop failed for {}", campaignId, e);
            }
        }, campaign.getDurationMinutes(), TimeUnit.MINUTES);

        activeRuns.put(campaignId, new ActiveRun(ctx, stopFuture, dispatchThread));

        webSocketPublisher.sendStatus(run.getId().toString(), "RUNNING");
        log.info("Started campaign '{}' — {} visits/h, {}m duration, {} simulation",
            campaign.getName(), campaign.getVisitsPerHour(), campaign.getDurationMinutes(), campaign.getSimulationLevel());

        return run;
    }

    @Transactional
    public CampaignRun stopCampaign(UUID campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));

        if (campaign.getStatus() != CampaignStatus.RUNNING && campaign.getStatus() != CampaignStatus.PAUSED) {
            throw new IllegalStateException("Campaign is not running or paused (current: " + campaign.getStatus() + ")");
        }

        ActiveRun active = activeRuns.remove(campaignId);
        if (active != null) {
            active.context().running().set(false);
            if (active.dispatchThread() != null) active.dispatchThread().interrupt();
            if (active.stopFuture() != null) active.stopFuture().cancel(false);
            active.context().visitExecutor().shutdownNow();
        }

        campaign.setStatus(CampaignStatus.COMPLETED);
        campaign.setLastRunAt(Instant.now());
        campaignRepository.save(campaign);

        CampaignRun run = null;
        if (active != null) {
            run = campaignRunRepository.findById(active.context().runId()).orElse(null);
        } else {
            var runs = campaignRunRepository.findByCampaignIdOrderByStartedAtDesc(campaignId);
            if (!runs.isEmpty() && runs.get(0).getStatus() == RunStatus.RUNNING) {
                run = runs.get(0);
            }
        }

        if (run != null) {
            run.setStatus(RunStatus.COMPLETED);
            run.setFinishedAt(Instant.now());
            if (active != null) {
                run.setTotalVisits(active.context().totalVisits().get());
                run.setSuccessfulVisits(active.context().successfulVisits().get());
                run.setFailedVisits(active.context().failedVisits().get());
            }
            run = campaignRunRepository.save(run);
            asocksService.cleanupPool("run-" + run.getId());
            webSocketPublisher.sendStatus(run.getId().toString(), "COMPLETED");
        }

        log.info("Stopped campaign '{}'", campaign.getName());
        return run;
    }

    @Transactional
    public void pauseCampaign(UUID campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));

        if (campaign.getStatus() != CampaignStatus.RUNNING) {
            throw new IllegalStateException("Campaign is not running (current: " + campaign.getStatus() + ")");
        }

        ActiveRun active = activeRuns.get(campaignId);
        if (active == null) {
            campaign.setStatus(CampaignStatus.COMPLETED);
            campaignRepository.save(campaign);
            throw new IllegalStateException("Campaign was lost due to server restart. It has been marked as completed.");
        }

        active.context().running().set(false);
        if (active.dispatchThread() != null) active.dispatchThread().interrupt();
        if (active.stopFuture() != null) active.stopFuture().cancel(false);

        flushStats(active.context());

        campaign.setStatus(CampaignStatus.PAUSED);
        campaignRepository.save(campaign);
        webSocketPublisher.sendStatus(active.context().runId().toString(), "PAUSED");
    }

    @Transactional
    public void resumeCampaign(UUID campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId)
            .orElseThrow(() -> new IllegalArgumentException("Campaign not found"));

        if (campaign.getStatus() != CampaignStatus.PAUSED) {
            throw new IllegalStateException("Campaign is not paused (current: " + campaign.getStatus() + ")");
        }

        ActiveRun active = activeRuns.get(campaignId);
        if (active == null) {
            campaign.setStatus(CampaignStatus.COMPLETED);
            campaignRepository.save(campaign);
            throw new IllegalStateException("Campaign was lost due to server restart. It has been marked as completed.");
        }

        active.context().running().set(true);
        campaign.setStatus(CampaignStatus.RUNNING);
        campaignRepository.save(campaign);

        long elapsedMinutes = Duration.between(active.context().startTime(), Instant.now()).toMinutes();
        int remainingMinutes = Math.max(1, campaign.getDurationMinutes() - (int) elapsedMinutes);
        ScheduledFuture<?> stopFuture = globalScheduler.schedule(() -> {
            try { stopCampaign(campaignId); } catch (Exception e) {
                log.error("Auto-stop failed for {}", campaignId, e);
            }
        }, remainingMinutes, TimeUnit.MINUTES);

        long intervalMs = calculateIntervalMs(campaign);
        String poolKey = "run-" + active.context().runId();
        Thread newDispatch = startDispatchLoop(active.context(), poolKey, campaign.getSimulationLevel(), intervalMs);

        activeRuns.put(campaignId, new ActiveRun(active.context(), stopFuture, newDispatch));
        webSocketPublisher.sendStatus(active.context().runId().toString(), "RUNNING");
    }

    public SimulationStats getStats(UUID campaignId) {
        ActiveRun active = activeRuns.get(campaignId);
        if (active == null) return null;

        var ctx = active.context();
        Duration elapsed = Duration.between(ctx.startTime(), Instant.now());
        long seconds = elapsed.getSeconds();
        double vps = seconds > 0 ? (double) ctx.totalVisits().get() / seconds : 0;

        return new SimulationStats(
            campaignId.toString(),
            ctx.runId().toString(),
            ctx.totalVisits().get(),
            ctx.successfulVisits().get(),
            ctx.failedVisits().get(),
            asocksService.availableProxies("run-" + ctx.runId()),
            Math.round(vps * 100.0) / 100.0,
            formatDuration(elapsed),
            "N/A"
        );
    }

    public boolean isRunning(UUID campaignId) {
        Campaign campaign = campaignRepository.findById(campaignId).orElse(null);
        return campaign != null && campaign.getStatus() == CampaignStatus.RUNNING;
    }

    public void shutdown() {
        log.info("Shutting down simulation engine...");
        for (var entry : new ArrayList<>(activeRuns.entrySet())) {
            try {
                entry.getValue().context().running().set(false);
                if (entry.getValue().dispatchThread() != null)
                    entry.getValue().dispatchThread().interrupt();
                if (entry.getValue().stopFuture() != null)
                    entry.getValue().stopFuture().cancel(false);
                entry.getValue().context().visitExecutor().shutdownNow();
            } catch (Exception e) {
                log.error("Failed to stop campaign {} during shutdown", entry.getKey(), e);
            }
        }
        activeRuns.clear();
        globalScheduler.shutdownNow();
    }

    private void executeVisit(SimulationContext ctx, String poolKey, SimulationLevel level) {
        GeoDistributionDto geo = pickGeo(ctx.campaign());
        String countryCode = geo != null ? geo.countryCode() : "US";

        String proxy = asocksService.acquireProxy(poolKey, countryCode);
        if (proxy == null) {
            log.warn("No proxy available for {} in pool '{}'", countryCode, poolKey);
            return;
        }

        try {
            boolean success = switch (level) {
                case HTTP_ONLY -> httpWorker.visitSimple(ctx.campaign(), proxy, countryCode, ctx);
                case BROWSER_NAVIGATION -> httpWorker.visitWithNavigation(ctx.campaign(), proxy, countryCode, ctx);
                case FULL_BROWSER -> browserWorker.visitFull(ctx.campaign(), proxy, countryCode, ctx);
            };

            // Retry once on failure with a fresh proxy
            if (!success) {
                asocksService.releaseProxy(poolKey, proxy, countryCode);
                proxy = asocksService.acquireProxy(poolKey, countryCode);
                if (proxy != null) {
                    success = switch (level) {
                        case HTTP_ONLY -> httpWorker.visitSimple(ctx.campaign(), proxy, countryCode, ctx);
                        case BROWSER_NAVIGATION -> httpWorker.visitWithNavigation(ctx.campaign(), proxy, countryCode, ctx);
                        case FULL_BROWSER -> browserWorker.visitFull(ctx.campaign(), proxy, countryCode, ctx);
                    };
                    if (!success) log.warn("Visit retry also failed");
                }
            }

            ctx.totalVisits().incrementAndGet();
            if (success) ctx.successfulVisits().incrementAndGet();
            else ctx.failedVisits().incrementAndGet();

            campaignRunRepository.findById(ctx.runId()).ifPresent(run -> {
                run.setTotalVisits(ctx.totalVisits().get());
                run.setSuccessfulVisits(ctx.successfulVisits().get());
                run.setFailedVisits(ctx.failedVisits().get());
                campaignRunRepository.save(run);
            });
        } catch (Exception e) {
            log.error("Visit execution failed", e);
            ctx.totalVisits().incrementAndGet();
            ctx.failedVisits().incrementAndGet();
        } finally {
            asocksService.releaseProxy(poolKey, proxy, countryCode);
        }
    }

    private Thread startDispatchLoop(SimulationContext ctx, String poolKey,
                                      SimulationLevel level, long intervalMs) {
        Thread t = Thread.ofVirtual().unstarted(() -> {
            java.util.concurrent.atomic.AtomicLong lastFlush = new java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis());
            while (ctx.running().get()) {
                Semaphore limit = ctx.concurrencyLimit();
                if (limit != null && limit.tryAcquire()) {
                    ctx.visitExecutor().submit(() -> {
                        try {
                            executeVisit(ctx, poolKey, level);
                        } catch (Exception e) {
                            log.warn("Visit failed: {}", e.getMessage());
                            ctx.totalVisits().incrementAndGet();
                            ctx.failedVisits().incrementAndGet();
                        } finally {
                            if (limit != null) limit.release();
                            long now = System.currentTimeMillis();
                            if (now - lastFlush.get() > 2000) {
                                webSocketPublisher.sendStats(ctx.runId().toString(), getStats(ctx.campaignId()));
                                lastFlush.set(now);
                            }
                        }
                    });
                }
                try {
                    Thread.sleep(Math.max(intervalMs, 50));
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        t.setName("dispatch-" + ctx.campaignId().toString().substring(0, 8));
        t.start();
        return t;
    }

    private void flushStats(SimulationContext ctx) {
        try {
            campaignRunRepository.findById(ctx.runId()).ifPresent(run -> {
                run.setTotalVisits(ctx.totalVisits().get());
                run.setSuccessfulVisits(ctx.successfulVisits().get());
                run.setFailedVisits(ctx.failedVisits().get());
                campaignRunRepository.save(run);
            });
            webSocketPublisher.sendStats(ctx.runId().toString(), getStats(ctx.campaignId()));
        } catch (Exception e) {
            log.error("Failed to flush stats", e);
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
