package com.clicker.service;

import com.clicker.config.AppProperties;
import com.clicker.domain.ProxyPort;
import com.clicker.repository.ProxyPortRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

@Service
public class AsocksService {

    private static final Logger log = LoggerFactory.getLogger(AsocksService.class);

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;
    private final ProxyPortRepository proxyPortRepository;

    private final Map<String, CountryPortPool> portPools = new ConcurrentHashMap<>();
    private final Map<String, PortInfo> portInfoCache = new ConcurrentHashMap<>();
    private static final int PORTS_PER_POOL = 20;
    private static final int POOL_REFILL_THRESHOLD = 3;

    public record PortInfo(int id, String server, int port, String login,
                           String password, String countryCode, long lastRefresh) {
        public String proxyUrl() {
            return "http://" + login + ":" + password + "@" + server + ":" + port;
        }
    }

    private static class CountryPortPool {
        final String countryCode;
        final BlockingDeque<PortInfo> available = new LinkedBlockingDeque<>();
        final Set<Integer> allPortIds = ConcurrentHashMap.newKeySet();
        volatile boolean shutdown;

        CountryPortPool(String countryCode) { this.countryCode = countryCode; }

        PortInfo poll() { return available.pollFirst(); }

        void add(PortInfo port) {
            if (!shutdown && port != null) {
                available.offer(port);
                allPortIds.add(port.id());
            }
        }

        int size() { return available.size(); }

        Set<Integer> allIds() { return Set.copyOf(allPortIds); }

        void drain() { available.clear(); }
    }

    public AsocksService(AppProperties appProperties, ObjectMapper objectMapper,
                          ProxyPortRepository proxyPortRepository) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.proxyPortRepository = proxyPortRepository;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build();
    }

    private String apiKey() { return appProperties.getAsocks().getApiKey(); }
    private String baseUrl() { return appProperties.getAsocks().getBaseUrl(); }

    public void initPool(String poolKey, Map<String, Integer> geoDistribution) {
        int totalWeight = geoDistribution.values().stream().mapToInt(Integer::intValue).sum();
        final int finalTotal = totalWeight > 0 ? totalWeight : 1;

        geoDistribution.forEach((countryCode, weight) -> {
            int count = Math.max(1, PORTS_PER_POOL * weight / finalTotal);
            String fullKey = poolKey + "-" + countryCode;
            portPools.computeIfAbsent(fullKey, k -> new CountryPortPool(countryCode));
            final int c = count;
            CompletableFuture.runAsync(() -> createAndFillPorts(poolKey, countryCode, c));
        });

        log.info("Initializing port pools for '{}' — {} countries", poolKey, geoDistribution.size());
    }

    private void createAndFillPorts(String poolKey, String countryCode, int count) {
        CountryPortPool pool = portPools.get(poolKey + "-" + countryCode);
        if (pool == null || pool.shutdown) return;

        int created = 0;
        for (int i = 0; i < count; i++) {
            PortInfo port = createPort(countryCode);
            if (port != null) {
                pool.add(port);
                portInfoCache.put(port.proxyUrl(), port);
                savePortToDb(poolKey, port);
                created++;
            }
        }

        log.info("Created {}/{} ports for {}/{} — pool size: {}",
            created, count, poolKey, countryCode, pool.size());

        // If we got fewer than requested, try once more
        if (created < count && created == 0) {
            log.warn("Retrying port creation for {}/{}", poolKey, countryCode);
            for (int i = 0; i < count; i++) {
                PortInfo port = createPort(countryCode);
                if (port != null) {
                    pool.add(port);
                    portInfoCache.put(port.proxyUrl(), port);
                }
            }
        }
    }

    private PortInfo createPort(String countryCode) {
        try {
            var body = new HashMap<String, Object>();
            body.put("country_code", countryCode);
            body.put("count", 1);
            body.put("type_id", 1);
            body.put("proxy_type_id", 1);

            JsonNode response = apiPost("/proxy/create-port", objectMapper.writeValueAsString(body));

            if (response.has("success") && response.get("success").asBoolean()
                && response.has("data") && response.get("data").isArray()
                && response.get("data").size() > 0) {

                JsonNode d = response.get("data").get(0);
                int id = d.get("id").asInt();
                String server = d.get("server").asText();
                int port = d.get("port").asInt();
                String login = d.get("login").asText();
                String password = d.get("password").asText();

                return new PortInfo(id, server, port, login, password, countryCode, 0);
            }
        } catch (Exception e) {
            log.error("Failed to create port for {}", countryCode, e);
        }
        return null;
    }

    private void savePortToDb(String poolKey, PortInfo portInfo) {
        try {
            var pp = new ProxyPort(
                portInfo.id(), poolKey, portInfo.countryCode(),
                portInfo.server(), portInfo.port(), Instant.now()
            );
            proxyPortRepository.save(pp);
        } catch (Exception e) {
            log.warn("Failed to save port {} to DB", portInfo.id(), e);
        }
    }

    public String acquireProxy(String poolKey, String countryCode) {
        CountryPortPool pool = portPools.get(poolKey + "-" + countryCode);
        if (pool == null) {
            pool = portPools.computeIfAbsent(poolKey + "-" + countryCode,
                k -> new CountryPortPool(countryCode));
        }

        PortInfo port = pool.poll();

        // Refill if low (but don't block — the createPort API is fast)
        if (pool.size() < POOL_REFILL_THRESHOLD) {
            int needed = PORTS_PER_POOL - pool.size();
            CompletableFuture.runAsync(() ->
                createAndFillPorts(poolKey, countryCode, needed));
        }

        return port != null ? port.proxyUrl() : null;
    }

    public void refreshProxy(String poolKey, String proxyUrl) {
        // Extract port ID from the pool and refresh it
        portPools.values().stream()
            .flatMap(p -> p.allIds().stream())
            .findAny()
            .ifPresent(this::refreshPortIp);
    }

    private void refreshPortIp(int portId) {
        try {
            apiGet("/proxy/refresh/" + portId, Map.of());
        } catch (Exception e) {
            log.warn("Failed to refresh port {}", portId);
        }
    }

    public void releaseProxy(String poolKey, String proxyUrl, String countryCode) {
        if (proxyUrl == null) return;
        PortInfo info = portInfoCache.get(proxyUrl);
        CountryPortPool pool = portPools.get(poolKey + "-" + countryCode);
        if (pool != null && info != null) {
            CompletableFuture.runAsync(() -> {
                refreshPortIp(info.id());
                try { Thread.sleep(500); } catch (InterruptedException ignored) {}
                pool.add(info);
            });
        }
    }

    public int availableProxies(String poolKey) {
        return portPools.entrySet().stream()
            .filter(e -> e.getKey().startsWith(poolKey + "-"))
            .mapToInt(e -> e.getValue().size())
            .sum();
    }

    public void cleanupPool(String poolKey) {
        portPools.entrySet().stream()
            .filter(e -> e.getKey().startsWith(poolKey + "-"))
            .forEach(e -> {
                e.getValue().shutdown = true;
                e.getValue().drain();
                e.getValue().allIds().forEach(this::deletePort);
            });
        portPools.entrySet().removeIf(e -> e.getKey().startsWith(poolKey + "-"));

        try {
            proxyPortRepository.deleteByPoolKey(poolKey);
        } catch (Exception e) {
            log.warn("Failed to delete port DB records for pool '{}'", poolKey);
        }

        log.info("Cleaned up port pool '{}'", poolKey);
    }

    private void deletePort(int portId) {
        try {
            apiGet("/proxy/port/" + portId, Map.of("_method", "DELETE"));
        } catch (Exception e) {
            log.warn("Failed to delete port {}", portId);
        }
    }

    public JsonNode apiGet(String path, Map<String, String> params) {
        try {
            HttpUrl.Builder urlBuilder = Objects.requireNonNull(
                HttpUrl.parse(baseUrl() + path)).newBuilder();
            urlBuilder.addQueryParameter("apiKey", apiKey());
            if (params != null) params.forEach(urlBuilder::addQueryParameter);

            Request request = new Request.Builder()
                .url(urlBuilder.build())
                .header("Content-Type", "application/json")
                .get()
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String body = response.body() != null ? response.body().string() : "{}";
                return objectMapper.readTree(body);
            }
        } catch (IOException e) {
            log.error("API call failed: {}", path, e);
            return objectMapper.createObjectNode().put("success", false);
        }
    }

    public JsonNode apiPost(String path, String jsonBody) {
        try {
            HttpUrl url = Objects.requireNonNull(HttpUrl.parse(baseUrl() + path))
                .newBuilder()
                .addQueryParameter("apiKey", apiKey())
                .build();

            RequestBody body = RequestBody.create(jsonBody, MediaType.get("application/json"));
            Request request = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .post(body)
                .build();

            try (Response response = httpClient.newCall(request).execute()) {
                String responseBody = response.body() != null ? response.body().string() : "{}";
                return objectMapper.readTree(responseBody);
            }
        } catch (IOException e) {
            log.error("API POST failed: {}", path, e);
            return objectMapper.createObjectNode().put("success", false);
        }
    }

    public void reconcileOrphanedPorts() {
        try {
            var orphaned = proxyPortRepository.findAllByOrderByCreatedAtAsc();
            if (orphaned.isEmpty()) return;

            log.info("Found {} orphaned port records from previous runs — cleaning up", orphaned.size());
            for (var pp : orphaned) {
                try {
                    deletePort(pp.getPortId());
                } catch (Exception e) {
                    log.warn("Failed to delete orphaned port {}", pp.getPortId());
                }
            }
            proxyPortRepository.deleteAll();
            log.info("Cleaned up {} orphaned ports", orphaned.size());
        } catch (Exception e) {
            log.warn("Failed to reconcile orphaned ports", e);
        }
    }

    public List<Map<String, String>> getCountries() {
        JsonNode response = apiGet("/dir/countries", Map.of());
        List<Map<String, String>> countries = new ArrayList<>();
        if (response.has("countries") && response.get("countries").isArray()) {
            response.get("countries").forEach(node -> {
                Map<String, String> c = new HashMap<>();
                c.put("code", node.has("code") ? node.get("code").asText() : "");
                c.put("name", node.has("name") ? node.get("name").asText() : "");
                countries.add(c);
            });
        }
        return countries;
    }

    public double getBalance() {
        JsonNode response = apiGet("/user/balance", Map.of());
        if (response.has("balance")) return response.get("balance").asDouble();
        return 0;
    }
}
