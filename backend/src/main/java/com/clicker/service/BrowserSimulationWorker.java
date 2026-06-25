package com.clicker.service;

import com.clicker.domain.*;
import com.clicker.repository.VisitEventRepository;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Proxy;
import com.microsoft.playwright.options.WaitUntilState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class BrowserSimulationWorker {

    private static final Logger log = LoggerFactory.getLogger(BrowserSimulationWorker.class);

    private final VisitEventRepository visitEventRepository;
    private final WebSocketPublisher webSocketPublisher;

    private Playwright playwright;
    private Browser sharedBrowser;
    private final Queue<BrowserContext> contextPool = new ConcurrentLinkedQueue<>();
    private final AtomicInteger activeContexts = new AtomicInteger(0);
    private static final int MAX_IDLE_CONTEXTS = 5;

    private static final String STEALTH_JS = """
        (() => {
            Object.defineProperty(navigator, 'webdriver', { get: () => false });
            Object.defineProperty(navigator, 'plugins', { get: () => [1,2,3,4,5] });
            Object.defineProperty(navigator, 'languages', { get: () => ['en-US', 'en'] });
            window.chrome = { runtime: {} };
            Object.defineProperty(navigator, 'hardwareConcurrency', { get: () => 8 });
            const getParams = () => ({ error: ()=>'' });
            Object.defineProperty(navigator, 'permissions', { get: () => ({ query: async () => ({ state: 'prompt' }) }) });
        })();
        """;

    public BrowserSimulationWorker(VisitEventRepository visitEventRepository,
                                   WebSocketPublisher webSocketPublisher) {
        this.visitEventRepository = visitEventRepository;
        this.webSocketPublisher = webSocketPublisher;
    }

    private synchronized Playwright getPlaywright() {
        if (playwright == null) {
            playwright = Playwright.create();
            sharedBrowser = playwright.chromium().launch(
                new BrowserType.LaunchOptions()
                    .setHeadless(true)
                    .setArgs(List.of(
                        "--no-sandbox",
                        "--disable-setuid-sandbox",
                        "--disable-dev-shm-usage",
                        "--disable-gpu",
                        "--disable-blink-features=AutomationControlled"
                    ))
            );
            log.info("Playwright browser pool initialized");
        }
        return playwright;
    }

    private BrowserContext acquireContext(String proxyUrl) {
        BrowserContext ctx = contextPool.poll();
        if (ctx != null) {
            activeContexts.decrementAndGet();
            return ctx;
        }

        getPlaywright();

        Browser.NewContextOptions options = new Browser.NewContextOptions()
            .setViewportSize(1920, 1080)
            .setLocale("en-US")
            .setTimezoneId("America/New_York")
            .setGeolocation(40.7128, -74.0060) // NYC
            .setPermissions(List.of("geolocation"))
            .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36");

        if (proxyUrl != null && !proxyUrl.isBlank()) {
            String proxyServer = proxyUrl;
            if (!proxyUrl.startsWith("http")) proxyServer = "http://" + proxyUrl;
            options.setProxy(new Proxy(proxyServer));
        }

        BrowserContext context = sharedBrowser.newContext(options);
        context.addInitScript(STEALTH_JS);

        return context;
    }

    private void setupPageForProfile(Page page, OrganicProfile profile) {
        page.evaluate("() => {"
            + "  Object.defineProperty(navigator, 'webdriver', { get: () => false });"
            + "  Object.defineProperty(navigator, 'plugins', { get: () => [1,2,3,4,5] });"
            + "  window.chrome = { runtime: {} };"
            + "}");
    }

    private void releaseContext(BrowserContext context) {
        if (contextPool.size() < MAX_IDLE_CONTEXTS) {
            contextPool.offer(context);
            activeContexts.incrementAndGet();
        } else {
            context.close();
        }
    }

    public boolean visitFull(Campaign campaign, String proxy,
                              String countryCode, SimulationEngine.SimulationContext context) {
        String baseUrl = campaign.getSite() != null ? campaign.getSite().getBaseUrl() : "";
        if (baseUrl.isEmpty()) return false;

        OrganicProfile profile = pickProfile(campaign);
        BrowserContext browserContext = null;
        Page page = null;
        long start = System.currentTimeMillis();

        try {
            browserContext = acquireContext(proxy);
            page = browserContext.newPage();
            setupPageForProfile(page, profile);

            page.navigate(baseUrl, new Page.NavigateOptions()
                .setTimeout(30000)
                .setWaitUntil(WaitUntilState.NETWORKIDLE));

            // Simulate viewing time
            realisticPause(2000, 5000);

            // Simulate mouse movement
            simulateNaturalMouseMovement(page);

            // Organic scroll: starts slow, accelerates, pauses
            organicScroll(page);

            // Simulate reading pause mid-page
            realisticPause(1000, 4000);

            // Sometimes click a random internal link
            if (ThreadLocalRandom.current().nextDouble() < 0.4) {
                clickRandomLink(page, baseUrl);
            }

            // Sometimes scroll back up slightly (bounce)
            if (ThreadLocalRandom.current().nextDouble() < 0.3) {
                scrollBackUp(page);
            }

            long responseTime = System.currentTimeMillis() - start;

            int statusCode = responseTime > 0 ? 200 : 0;
            saveVisitEvent(context.runId(), proxy, countryCode, "/",
                statusCode, (int) responseTime, true, null, profile);
            webSocketPublisher.sendVisit(
                context.runId().toString(), context.campaignId().toString(),
                "/", statusCode, (int) responseTime, true, proxy
            );

            return true;
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - start;
            saveVisitEvent(context.runId(), proxy, countryCode, "/",
                null, (int) responseTime, false, e.getMessage(), profile);
            webSocketPublisher.sendVisit(
                context.runId().toString(), context.campaignId().toString(),
                "/", null, (int) responseTime, false, proxy
            );
            return false;
        } finally {
            if (page != null) {
                try { page.close(); } catch (Exception ignored) {}
            }
            if (browserContext != null) {
                releaseContext(browserContext);
            }
        }
    }

    private void organicScroll(Page page) {
        try {
            page.evaluate("async () => {"
                + "const totalH = document.body.scrollHeight;"
                + "const viewH = window.innerHeight;"
                + "const steps = 10 + Math.floor(Math.random() * 8);"
                + "const stepSize = (totalH - viewH) / steps;"
                + "for (let i = 1; i <= steps; i++) {"
                + "  const target = stepSize * i;"
                + "  const duration = 200 + Math.floor(Math.random() * 400);"
                + "  await new Promise(r => {"
                + "    const start = window.scrollY;"
                + "    const startTime = performance.now();"
                + "    const animate = (now) => {"
                + "      const progress = Math.min((now - startTime) / duration, 1);"
                + "      const eased = progress < 0.5 ? 2*progress*progress : 1 - Math.pow(-2*progress + 2, 2)/2;"
                + "      window.scrollTo(0, start + (target - start) * eased);"
                + "      if (progress < 1) requestAnimationFrame(animate);"
                + "      else r();"
                + "    };"
                + "    requestAnimationFrame(animate);"
                + "  });"
                + "  await new Promise(r => setTimeout(r, 300 + Math.floor(Math.random() * 800)));"
                + "}"
                + "}");
            page.waitForTimeout(500);
        } catch (Exception ignored) {}
    }

    private void scrollBackUp(Page page) {
        try {
            page.evaluate("async () => {"
                + "const current = window.scrollY;"
                + "const goUp = current * (0.1 + Math.random() * 0.2);"
                + "const target = Math.max(0, current - goUp);"
                + "const duration = 300 + Math.floor(Math.random() * 400);"
                + "await new Promise(r => {"
                + "  const start = window.scrollY;"
                + "  const startTime = performance.now();"
                + "  const animate = (now) => {"
                + "    const progress = Math.min((now - startTime) / duration, 1);"
                + "    window.scrollTo(0, start + (target - start) * progress);"
                + "    if (progress < 1) requestAnimationFrame(animate);"
                + "    else r();"
                + "  };"
                + "  requestAnimationFrame(animate);"
                + "});"
                + "}");
        } catch (Exception ignored) {}
    }

    private void simulateNaturalMouseMovement(Page page) {
        try {
            page.evaluate("async () => {"
                + "const move = (x, y) => {"
                + "  const evt = new MouseEvent('mousemove', { clientX: x, clientY: y, bubbles: true });"
                + "  document.dispatchEvent(evt);"
                + "};"
                + "const w = window.innerWidth;"
                + "const h = window.innerHeight;"
                + "let cx = w * 0.3, cy = h * 0.3;"
                + "for (let i = 0; i < 5; i++) {"
                + "  const tx = w * (0.1 + Math.random() * 0.8);"
                + "  const ty = h * (0.1 + Math.random() * 0.6);"
                + "  const steps = 10 + Math.floor(Math.random() * 10);"
                + "  for (let s = 1; s <= steps; s++) {"
                + "    const px = cx + (tx - cx) * (s / steps) + (Math.random() - 0.5) * 10;"
                + "    const py = cy + (ty - cy) * (s / steps) + (Math.random() - 0.5) * 10;"
                + "    move(px, py);"
                + "    await new Promise(r => setTimeout(r, 5 + Math.floor(Math.random() * 10)));"
                + "  }"
                + "  cx = tx; cy = ty;"
                + "  await new Promise(r => setTimeout(r, 200 + Math.floor(Math.random() * 500)));"
                + "}"
                + "}");
        } catch (Exception ignored) {}
    }

    private void clickRandomLink(Page page, String baseUrl) {
        try {
            Object result = page.evaluate("() => {"
                + "const links = document.querySelectorAll('a[href]');"
                + "const internal = Array.from(links).filter(a => a.href.startsWith(window.location.origin));"
                + "if (internal.length === 0) return null;"
                + "const link = internal[Math.floor(Math.random() * internal.length)];"
                + "return link.href;"
                + "}");

            if (result instanceof String url && url != null && !url.isBlank()) {
                page.navigate(url, new Page.NavigateOptions()
                    .setTimeout(15000)
                    .setWaitUntil(WaitUntilState.NETWORKIDLE));
                realisticPause(1000, 4000);
            }
        } catch (Exception ignored) {}
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

    private void realisticPause(int minMs, int maxMs) {
        try {
            Thread.sleep(ThreadLocalRandom.current().nextInt(minMs, maxMs));
        } catch (InterruptedException ignored) {}
    }

    public void shutdown() {
        contextPool.forEach(BrowserContext::close);
        contextPool.clear();
        if (sharedBrowser != null) {
            try { sharedBrowser.close(); } catch (Exception ignored) {}
            sharedBrowser = null;
        }
        if (playwright != null) {
            try { playwright.close(); } catch (Exception ignored) {}
            playwright = null;
        }
        log.info("Playwright browser pool shut down");
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
