package com.clicker;

import com.clicker.service.BrowserSimulationWorker;
import com.clicker.service.SimulationEngine;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ClickerApplication {

    private final SimulationEngine simulationEngine;
    private final BrowserSimulationWorker browserWorker;

    public ClickerApplication(SimulationEngine simulationEngine, BrowserSimulationWorker browserWorker) {
        this.simulationEngine = simulationEngine;
        this.browserWorker = browserWorker;
    }

    public static void main(String[] args) {
        SpringApplication.run(ClickerApplication.class, args);
    }

    @EventListener(ContextClosedEvent.class)
    public void onShutdown() {
        simulationEngine.shutdown();
        browserWorker.shutdown();
    }
}
