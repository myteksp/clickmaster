package com.clicker.service;

import com.clicker.config.AppProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

@Service
public class AsocksService {

    private static final Logger log = LoggerFactory.getLogger(AsocksService.class);

    private final AppProperties appProperties;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;

    private final Map<String, PerCountryPool> pools = new ConcurrentHashMap<>();

    private static final int POOL_TARGET_SIZE = 15;
    private static final int POOL_REFILL_THRESHOLD = 5;

    public AsocksService(AppProperties appProperties, ObjectMapper objectMapper) {
        this.appProperties = appProperties;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build();
    }

    private String apiKey() {
        return appProperties.getAsocks().getApiKey();
    }

    private String baseUrl() {
        return appProperties.getAsocks().getBaseUrl();
    }

    private static class PerCountryPool {
        final String countryCode;
        final BlockingDeque<String> proxies = new LinkedBlockingDeque<>();
        volatile boolean shutdown;

        PerCountryPool(String countryCode) {
            this.countryCode = countryCode;
        }

        String poll() {
            return proxies.pollFirst();
        }

        void add(String proxy) {
            if (!shutdown && proxy != null && !proxy.isBlank()) {
                proxies.offer(proxy);
            }
        }

        int size() { return proxies.size(); }

        void drain() { proxies.clear(); }
    }

    public String acquireProxy(String poolKey, String countryCode) {
        PerCountryPool pool = pools.computeIfAbsent(poolKey + "-" + countryCode,
            k -> new PerCountryPool(countryCode));

        String proxy = pool.poll();
        if (proxy == null) {
            log.debug("Pool {} empty for {}, fetching more", poolKey, countryCode);
            refillPool(poolKey, countryCode, POOL_TARGET_SIZE);
            proxy = pool.poll();
        }

        if (proxy != null && pool.size() < POOL_REFILL_THRESHOLD) {
            CompletableFuture.runAsync(() -> refillPool(poolKey, countryCode, POOL_TARGET_SIZE));
        }

        return proxy;
    }

    private void refillPool(String poolKey, String countryCode, int count) {
        PerCountryPool pool = pools.get(poolKey + "-" + countryCode);
        if (pool == null || pool.shutdown) return;

        List<String> fresh = searchProxies(countryCode, count);
        fresh.forEach(pool::add);

        if (!fresh.isEmpty()) {
            log.debug("Fetched {} proxies for {}/{}", fresh.size(), poolKey, countryCode);
        }
    }

    public List<String> searchProxies(String countryCode, int limit) {
        JsonNode response = apiGet("/proxy/search", Map.of(
            "country", countryCode != null ? countryCode : "US",
            "limit", String.valueOf(Math.min(limit, 20))
        ));

        List<String> proxies = new ArrayList<>();

        if (response.isArray() && response.size() > 0) {
            JsonNode first = response.get(0);
            if (first.has("success") && first.get("success").asBoolean()) {
                for (int i = 1; i < response.size(); i++) {
                    String proxy = response.get(i).asText();
                    if (proxy != null && !proxy.isBlank()) {
                        proxies.add(proxy);
                    }
                }
            }
        } else if (response.isObject() && response.has("success") && response.get("success").asBoolean()) {
            response.fieldNames().forEachRemaining(field -> {
                if (!"success".equals(field)) {
                    String proxy = response.get(field).asText();
                    if (proxy != null && !proxy.isBlank()) {
                        proxies.add(proxy);
                    }
                }
            });
        }
        return proxies;
    }

    public void initPool(String poolKey, Map<String, Integer> geoDistribution) {
        int totalWeight = geoDistribution.values().stream().mapToInt(Integer::intValue).sum();
        final int finalTotal = totalWeight > 0 ? totalWeight : POOL_TARGET_SIZE;

        geoDistribution.forEach((countryCode, weight) -> {
            int count = Math.max(1, POOL_TARGET_SIZE * weight / finalTotal);
            String fullKey = poolKey + "-" + countryCode;
            pools.computeIfAbsent(fullKey, k -> new PerCountryPool(countryCode));
            refillPool(poolKey, countryCode, count);
        });

        long total = pools.entrySet().stream()
            .filter(e -> e.getKey().startsWith(poolKey + "-"))
            .mapToLong(e -> e.getValue().size())
            .sum();
        log.info("Initialized proxy pool '{}' with {} total proxies across {} countries",
            poolKey, total, geoDistribution.size());
    }

    public int availableProxies(String poolKey) {
        return pools.entrySet().stream()
            .filter(e -> e.getKey().startsWith(poolKey + "-"))
            .mapToInt(e -> e.getValue().size())
            .sum();
    }

    public void cleanupPool(String poolKey) {
        pools.entrySet().stream()
            .filter(e -> e.getKey().startsWith(poolKey + "-"))
            .forEach(e -> {
                e.getValue().shutdown = true;
                e.getValue().drain();
            });
        pools.entrySet().removeIf(e -> e.getKey().startsWith(poolKey + "-"));
        log.info("Cleaned up proxy pool '{}'", poolKey);
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
            log.error("asocks API call failed: {}", path, e);
            return objectMapper.createObjectNode().put("success", false);
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
