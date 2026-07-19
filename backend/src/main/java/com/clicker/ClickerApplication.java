package com.clicker;

import com.clicker.domain.Campaign;
import com.clicker.domain.CampaignStatus;
import com.clicker.domain.CampaignRun;
import com.clicker.domain.RunStatus;
import com.clicker.repository.CampaignRepository;
import com.clicker.repository.CampaignRunRepository;
import com.clicker.service.AsocksService;
import com.clicker.service.BrowserSimulationWorker;
import com.clicker.service.SimulationEngine;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Instant;
import java.util.List;

@SpringBootApplication
@EnableScheduling
public class ClickerApplication {

    private static final Logger log = LoggerFactory.getLogger(ClickerApplication.class);

    private final SimulationEngine simulationEngine;
    private final BrowserSimulationWorker browserWorker;
    private final AsocksService asocksService;
    private final CampaignRepository campaignRepository;
    private final CampaignRunRepository campaignRunRepository;

    public ClickerApplication(SimulationEngine simulationEngine,
                              BrowserSimulationWorker browserWorker,
                              AsocksService asocksService,
                              CampaignRepository campaignRepository,
                              CampaignRunRepository campaignRunRepository) {
        this.simulationEngine = simulationEngine;
        this.browserWorker = browserWorker;
        this.asocksService = asocksService;
        this.campaignRepository = campaignRepository;
        this.campaignRunRepository = campaignRunRepository;
    }

    public static void main(String[] args) {
        SpringApplication.run(ClickerApplication.class, args);
    }

    @PostConstruct
    void init() {
        new Thread(() -> browserWorker.warmUp(), "playwright-init").start();
        new Thread(() -> {
            try { Thread.sleep(3000); } catch (InterruptedException ignored) {}
            asocksService.reconcileOrphanedPorts();
        }, "port-reconcile").start();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        reconcileStaleCampaigns();
    }

    private void reconcileStaleCampaigns() {
        List<Campaign> stale = campaignRepository.findAll().stream()
            .filter(c -> c.getStatus() == CampaignStatus.RUNNING || c.getStatus() == CampaignStatus.PAUSED)
            .toList();

        if (stale.isEmpty()) return;

        log.warn("Found {} stale RUNNING/PAUSED campaigns from previous run — marking as COMPLETED", stale.size());

        for (Campaign c : stale) {
            c.setStatus(CampaignStatus.COMPLETED);
            c.setLastRunAt(Instant.now());
            campaignRepository.save(c);

            campaignRunRepository.findByCampaignIdOrderByStartedAtDesc(c.getId()).stream()
                .filter(r -> r.getStatus() == RunStatus.RUNNING)
                .findFirst()
                .ifPresent(run -> {
                    run.setStatus(RunStatus.COMPLETED);
                    run.setFinishedAt(Instant.now());
                    campaignRunRepository.save(run);
                });
        }

        log.info("Reconciled {} stale campaigns", stale.size());
    }

    @EventListener(ContextClosedEvent.class)
    public void onShutdown() {
        simulationEngine.shutdown();
        browserWorker.shutdown();
    }
}
