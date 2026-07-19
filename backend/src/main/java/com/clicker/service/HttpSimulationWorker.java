package com.clicker.service;

import com.clicker.domain.*;
import com.clicker.repository.VisitEventRepository;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Component
public class HttpSimulationWorker {

    private static final Logger log = LoggerFactory.getLogger(HttpSimulationWorker.class);

    private final VisitEventRepository visitEventRepository;
    private final WebSocketPublisher webSocketPublisher;

    private static final List<String> COMMON_PATHS = List.of(
        "/", "/about", "/contact", "/pricing", "/blog",
        "/products", "/services", "/faq", "/login", "/register",
        "/terms", "/privacy", "/features", "/docs", "/support"
    );

    public HttpSimulationWorker(VisitEventRepository visitEventRepository,
                                WebSocketPublisher webSocketPublisher) {
        this.visitEventRepository = visitEventRepository;
        this.webSocketPublisher = webSocketPublisher;
    }

    public boolean visitSimple(Campaign campaign, String proxy,
                                String countryCode, SimulationEngine.SimulationContext context) {
        String baseUrl = campaign.getSite() != null ? campaign.getSite().getBaseUrl() : "";
        if (baseUrl.isEmpty()) return false;

        OrganicProfile profile = pickProfile(campaign);
        CookieSession cookies = new CookieSession();
        OkHttpClient client = buildHttpClient(proxy, profile, cookies);

        long start = System.currentTimeMillis();
        try {
            Request request = buildRequest(baseUrl, null, profile);
            try (Response response = client.newCall(request).execute()) {
                int status = response.code();
                long responseTime = System.currentTimeMillis() - start;

                saveVisitEvent(context.runId(), proxy, countryCode, "/",
                    status, (int) responseTime, true, null, profile);
                webSocketPublisher.sendVisit(
                    context.runId().toString(), context.campaignId().toString(),
                    "/", status, (int) responseTime, true, maskProxy(proxy)
                );

                // Simulate reading time
                realisticPause(2000, 8000);

                return status >= 200 && status < 400;
            }
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - start;
            saveVisitEvent(context.runId(), proxy, countryCode, "/",
                null, (int) responseTime, false, e.getMessage(), profile);
            webSocketPublisher.sendVisit(
                context.runId().toString(), context.campaignId().toString(),
                "/", null, (int) responseTime, false, maskProxy(proxy)
            );
            return false;
        }
    }

    public boolean visitWithNavigation(Campaign campaign, String proxy,
                                        String countryCode, SimulationEngine.SimulationContext context) {
        String baseUrl = campaign.getSite() != null ? campaign.getSite().getBaseUrl() : "";
        if (baseUrl.isEmpty()) return false;

        OrganicProfile profile = pickProfile(campaign);
        CookieSession cookies = new CookieSession();
        OkHttpClient client = buildHttpClient(proxy, profile, cookies);
        Random random = ThreadLocalRandom.current();
        boolean overallSuccess = true;
        String previousUrl = null;

        List<String> paths = generatePaths(baseUrl);

        for (int i = 0; i < paths.size(); i++) {
            String path = paths.get(i);
            long start = System.currentTimeMillis();

            try {
                Request request = buildRequest(path, previousUrl, profile);
                try (Response response = client.newCall(request).execute()) {
                    int status = response.code();
                    long responseTime = System.currentTimeMillis() - start;

                    saveVisitEvent(context.runId(), proxy, countryCode, path,
                        status, (int) responseTime, true, null, profile);
                    webSocketPublisher.sendVisit(
                        context.runId().toString(), context.campaignId().toString(),
                        path, status, (int) responseTime, true, maskProxy(proxy)
                    );

                    if (status < 200 || status >= 400) overallSuccess = false;
                    previousUrl = path;

                    // Realistic pause between page views
                    if (i < paths.size() - 1) {
                        realisticPause(1500, 6000);
                    }
                }
            } catch (Exception e) {
                long responseTime = System.currentTimeMillis() - start;
                saveVisitEvent(context.runId(), proxy, countryCode, path,
                    null, (int) responseTime, false, e.getMessage(), profile);
                webSocketPublisher.sendVisit(
                    context.runId().toString(), context.campaignId().toString(),
                    path, null, (int) responseTime, false, maskProxy(proxy)
                );
                overallSuccess = false;
            }
        }
        return overallSuccess;
    }

    private Request buildRequest(String url, String referer, OrganicProfile profile) {
        var builder = new Request.Builder()
            .url(url)
            .header("User-Agent", profile.userAgent())
            .header("Accept", profile.accept().get(0))
            .header("Accept-Language", profile.acceptLanguage())
            .header("Accept-Encoding", profile.acceptEncoding())
            .header("Cache-Control", "max-age=0")
            .header("Connection", "keep-alive")
            .header("DNT", "1")
            .header("Upgrade-Insecure-Requests", "1");

        if (profile.secChUa() != null) {
            builder.header("Sec-Ch-Ua", profile.secChUa());
            builder.header("Sec-Ch-Ua-Mobile", profile.secChUaMobile());
            builder.header("Sec-Ch-Ua-Platform", profile.secChUaPlatform());
        }

        builder.header("Sec-Fetch-Dest", "document");
        builder.header("Sec-Fetch-Mode", "navigate");
        builder.header("Sec-Fetch-Site", referer != null ? "same-origin" : "none");
        builder.header("Sec-Fetch-User", "?1");

        if (referer != null) {
            builder.header("Referer", referer);
        }

        if (profile.engine() == OrganicProfile.BrowserEngine.GECKO) {
            builder.header("TE", "trailers");
        }

        return builder.get().build();
    }

    private OkHttpClient buildHttpClient(String proxyUrl, OrganicProfile profile, CookieSession cookies) {
        if (proxyUrl == null || proxyUrl.isBlank()) {
            throw new IllegalArgumentException("No proxy provided — refusing to send traffic from server IP");
        }

        String host;
        int port;
        String proxyUser = null;
        String proxyPass = null;

        try {
            if (proxyUrl.contains("://")) {
                URI uri = URI.create(proxyUrl);
                host = uri.getHost();
                port = uri.getPort() > 0 ? uri.getPort() : 80;
                String userInfo = uri.getUserInfo();
                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    proxyUser = parts[0];
                    proxyPass = parts[1];
                }
            } else {
                String[] parts = proxyUrl.split(":");
                host = parts[0];
                port = parts.length > 1 ? Integer.parseInt(parts[1]) : 80;
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to parse proxy URL: " + proxyUrl, e);
        }

        final String finalUser = proxyUser;
        final String finalPass = proxyPass;

        var builder = baseClient(cookies)
            .newBuilder()
            .proxy(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port)));

        if (finalUser != null && finalPass != null) {
            builder.proxyAuthenticator((route, response) -> {
                String credential = Credentials.basic(finalUser, finalPass);
                return response.request().newBuilder()
                    .header("Proxy-Authorization", credential)
                    .build();
            });
        }

        return builder.build();
    }

    private OkHttpClient baseClient(CookieSession cookies) {
        return new OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(false)
            .cookieJar(cookies)
            .protocols(List.of(Protocol.HTTP_2, Protocol.HTTP_1_1))  // Prefer H2 like real browsers
            .build();
    }

    private OrganicProfile pickProfile(Campaign campaign) {
        try {
            var deviceProfileJson = campaign.getDeviceProfile();
            if (deviceProfileJson != null && !deviceProfileJson.equals("[]")) {
                var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                var deviceProfiles = mapper.readValue(deviceProfileJson,
                    new com.fasterxml.jackson.core.type.TypeReference<List<com.clicker.dto.DeviceProfileDto>>() {});
                if (!deviceProfiles.isEmpty()) {
                    var dp = deviceProfiles.get(ThreadLocalRandom.current().nextInt(deviceProfiles.size()));
                    return OrganicProfile.randomMatching(dp.device(), dp.browser());
                }
            }
        } catch (Exception ignored) {}
        return OrganicProfile.random();
    }

    private List<String> generatePaths(String baseUrl) {
        String clean = baseUrl.replaceAll("/$", "");
        List<String> result = new ArrayList<>();
        Random random = ThreadLocalRandom.current();
        int pathCount = 2 + random.nextInt(4);

        result.add(clean + "/");
        for (int i = 0; i < pathCount; i++) {
            result.add(clean + COMMON_PATHS.get(random.nextInt(COMMON_PATHS.size())));
        }
        return result;
    }

    private void realisticPause(int minMs, int maxMs) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(minMs, maxMs));
        } catch (InterruptedException ignored) {}
    }

    private void saveVisitEvent(UUID runId, String proxy, String countryCode,
                                 String path, Integer statusCode, int responseTimeMs,
                                 boolean success, String errorMessage, OrganicProfile profile) {
        try {
            var event = VisitEvent.builder()
                .campaignRunId(runId)
                .proxyAddress(maskProxy(proxy))
                .countryCode(countryCode)
                .path(path)
                .statusCode(statusCode)
                .responseTimeMs(responseTimeMs)
                .success(success)
                .errorMessage(errorMessage)
                .createdAt(Instant.now())
                .build();
            visitEventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to save visit event", e);
        }
    }

    private String maskProxy(String proxy) {
        if (proxy == null) return null;
        int atIdx = proxy.lastIndexOf('@');
        if (atIdx > 0) return proxy.substring(atIdx + 1);
        return proxy;
    }
}
