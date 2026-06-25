package com.clicker.service;

import com.clicker.dto.SimulationStats;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class WebSocketPublisher {

    private static final Logger log = LoggerFactory.getLogger(WebSocketPublisher.class);

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void sendVisit(String runId, String campaignId, String path,
                          Integer statusCode, int responseTimeMs,
                          boolean success, String proxyAddress) {
        var payload = Map.of(
            "type", "visit",
            "runId", runId,
            "campaignId", campaignId,
            "path", path != null ? path : "/",
            "statusCode", statusCode != null ? statusCode : 0,
            "responseTimeMs", responseTimeMs,
            "success", success,
            "proxyAddress", maskProxy(proxyAddress)
        );
        messagingTemplate.convertAndSend("/topic/visits/" + runId, payload);
    }

    public void sendStats(String runId, SimulationStats stats) {
        messagingTemplate.convertAndSend("/topic/stats/" + runId, stats);
    }

    public void sendStatus(String runId, String status) {
        var payload = Map.of("type", "status", "runId", runId, "status", status);
        messagingTemplate.convertAndSend("/topic/status/" + runId, payload);
    }

    private String maskProxy(String proxy) {
        if (proxy == null) return null;
        int colonIdx = proxy.lastIndexOf(':');
        if (colonIdx > 0) {
            return proxy.substring(0, colonIdx) + ":****";
        }
        return proxy;
    }
}
