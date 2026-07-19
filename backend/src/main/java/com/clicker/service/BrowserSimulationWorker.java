package com.clicker.service;

import com.clicker.domain.*;
import com.clicker.dto.ClickTargetDto;
import com.clicker.repository.VisitEventRepository;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.Geolocation;
import com.microsoft.playwright.options.LoadState;
import com.microsoft.playwright.options.Proxy;
import com.microsoft.playwright.options.ScreenshotType;
import com.microsoft.playwright.options.WaitUntilState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.time.Instant;
import java.util.*;
import java.util.Base64;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import com.clicker.dto.NavigationStepDto;
import com.clicker.dto.SessionState;

@Component
public class BrowserSimulationWorker {

    private static final Logger log = LoggerFactory.getLogger(BrowserSimulationWorker.class);

    private final VisitEventRepository visitEventRepository;
    private final WebSocketPublisher webSocketPublisher;

    private Playwright playwright;
    private Browser sharedBrowser;

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
                        "--disable-blink-features=AutomationControlled",
                        "--disable-features=IsolateOrigins,site-per-process"
                    ))
            );
            log.info("Playwright browser launched");
        }
        return playwright;
    }

    private BrowserContext acquireContext(String proxyUrl, OrganicProfile profile, String countryCode) {
        getPlaywright();

        Browser.NewContextOptions options = new Browser.NewContextOptions()
            .setViewportSize(profile.viewportWidth(), profile.viewportHeight())
            .setLocale(countryLocale(countryCode, profile))
            .setTimezoneId(countryTimezone(countryCode, profile))
            .setGeolocation(countryGeo(countryCode, profile))
            .setPermissions(List.of("geolocation"))
            .setUserAgent(profile.userAgent())
            .setDeviceScaleFactor(profile.devicePixelRatio());

        if (profile.secChUa() != null && profile.secChUaPlatform() != null) {
            options.setExtraHTTPHeaders(Map.of(
                "sec-ch-ua", profile.secChUa(),
                "sec-ch-ua-mobile", profile.secChUaMobile(),
                "sec-ch-ua-platform", profile.secChUaPlatform(),
                "accept-language", countryAcceptLanguage(countryCode, profile)
            ));
        }

        if (proxyUrl != null && !proxyUrl.isBlank()) {
            ParsedProxy parsed = ParsedProxy.parse(proxyUrl);
            Proxy proxy = new Proxy(parsed.server);
            if (parsed.username != null) {
                proxy.setUsername(parsed.username);
                proxy.setPassword(parsed.password);
            }
            options.setProxy(proxy);
        }

        BrowserContext context = sharedBrowser.newContext(options);

        context.addInitScript(buildFingerprintScript(profile));

        return context;
    }

    private String buildFingerprintScript(OrganicProfile profile) {
        return "(function() {"
            + "  Object.defineProperty(navigator, 'webdriver', {get: () => undefined});"
            + "  Object.defineProperty(navigator, 'platform', {get: () => '" + profile.platform() + "'});"
            + "  Object.defineProperty(navigator, 'deviceMemory', {get: () => 8});"
            + "  Object.defineProperty(navigator, 'hardwareConcurrency', {get: () => 8});"
            + "  Object.defineProperty(navigator, 'languages', {get: () => " + toJsonArray(profile.languages()) + "});"
            + "  window.chrome = { runtime: {} };"
            + "  const originalQuery = window.navigator.permissions.query;"
            + "  window.navigator.permissions.query = (params) =>"
            + "    params.name === 'notifications'"
            + "      ? Promise.resolve({state: Notification.permission})"
            + "      : originalQuery(params);"
            + "  const getParameter = WebGLRenderingContext.prototype.getParameter;"
            + "  WebGLRenderingContext.prototype.getParameter = function(p) {"
            + "    if (p === 37445) return '" + (profile.webglVendor() != null ? profile.webglVendor() : "Google Inc. (Intel)") + "';"
            + "    if (p === 37446) return '" + (profile.webglRenderer() != null ? profile.webglRenderer() : "ANGLE (Intel, Intel(R) UHD Graphics 630 Direct3D11 vs_5_0 ps_5_0)") + "';"
            + "    return getParameter.call(this, p);"
            + "  };"
            + "})();";
    }

    private static String toJsonArray(List<String> list) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) sb.append(",");
            sb.append("'").append(list.get(i)).append("'");
        }
        sb.append("]");
        return sb.toString();
    }

    private record ParsedProxy(String server, String username, String password) {
        static ParsedProxy parse(String proxyUrl) {
            try {
                String url = proxyUrl;
                if (!url.startsWith("http")) url = "http://" + url;
                URI uri = URI.create(url);
                String server = uri.getScheme() + "://" + uri.getHost() + ":" +
                    (uri.getPort() > 0 ? uri.getPort() : 80);
                String userInfo = uri.getUserInfo();
                if (userInfo != null && userInfo.contains(":")) {
                    String[] parts = userInfo.split(":", 2);
                    return new ParsedProxy(server, parts[0], parts[1]);
                }
                return new ParsedProxy(server, null, null);
            } catch (Exception e) {
                return new ParsedProxy(proxyUrl, null, null);
            }
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
            browserContext = acquireContext(proxy, profile, countryCode);
            page = browserContext.newPage();

            Response response = page.navigate(baseUrl, new Page.NavigateOptions()
                .setTimeout(30000)
                .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));

            int statusCode = response != null ? response.status() : 0;

            page.waitForTimeout(1000);

            acceptCookieConsent(page);

            page.waitForTimeout(2000);

            moveMouseTrusted(page);

            scrollDown(page);
            page.waitForTimeout(2000);

            scrollUp(page);
            page.waitForTimeout(1000);

            List<ClickTargetDto> targets = parseClickTargets(campaign);
            if (!targets.isEmpty()) {
                executeClickTargets(page, baseUrl, targets);
            } else if (ThreadLocalRandom.current().nextDouble() < 0.5) {
                clickRandomLink(page, baseUrl);
            }

            long responseTime = System.currentTimeMillis() - start;

            saveVisitEvent(context.runId(), proxy, countryCode, "/",
                statusCode, (int) responseTime, true, null, profile);
            webSocketPublisher.sendVisit(
                context.runId().toString(), context.campaignId().toString(),
                "/", statusCode, (int) responseTime, true, maskProxy(proxy)
            );

            return true;
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - start;
            String errorMsg = e.getMessage();
            if (errorMsg != null && errorMsg.length() > 500) {
                errorMsg = errorMsg.substring(0, 500);
            }
            saveVisitEvent(context.runId(), proxy, countryCode, "/",
                null, (int) responseTime, false, errorMsg, profile);
            webSocketPublisher.sendVisit(
                context.runId().toString(), context.campaignId().toString(),
                "/", null, (int) responseTime, false, maskProxy(proxy)
            );
            return false;
        } finally {
            if (page != null) {
                try { page.close(); } catch (Exception ignored) {}
            }
            if (browserContext != null) {
                try { browserContext.close(); } catch (Exception ignored) {}
            }
        }
    }

    private void scrollDown(Page page) {
        try {
            int steps = 12;
            int viewportH = 1080;
            for (int i = 1; i <= steps; i++) {
                int target = (int) (i * (double) viewportH / steps * 1.5);
                page.evaluate("(t) => window.scrollTo({top: t, behavior: 'smooth'})", target);
                page.waitForTimeout(400 + ThreadLocalRandom.current().nextInt(200));
            }
        } catch (Exception ignored) {}
    }

    private void scrollUp(Page page) {
        try {
            int steps = 10;
            for (int i = steps; i >= 0; i--) {
                int target = (int) (i * (double) 1080 / steps * 1.5);
                page.evaluate("(t) => window.scrollTo({top: t, behavior: 'smooth'})", target);
                page.waitForTimeout(350 + ThreadLocalRandom.current().nextInt(150));
            }
        } catch (Exception ignored) {}
    }

    private void moveMouseTrusted(Page page) {
        try {
            for (int i = 0; i < 6; i++) {
                double x = 100 + ThreadLocalRandom.current().nextDouble(800);
                double y = 100 + ThreadLocalRandom.current().nextDouble(500);
                page.mouse().move(x, y);
                page.waitForTimeout(100 + ThreadLocalRandom.current().nextInt(200));
            }
        } catch (Exception ignored) {}
    }

    private List<ClickTargetDto> parseClickTargets(Campaign campaign) {
        try {
            var mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(campaign.getClickTargets(),
                new com.fasterxml.jackson.core.type.TypeReference<List<ClickTargetDto>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private void executeClickTargets(Page page, String baseUrl, List<ClickTargetDto> targets) {
        String currentUrl = page.url();
        for (ClickTargetDto target : targets) {
            try {
                int prob = target.probability();
                if (prob <= 0 || ThreadLocalRandom.current().nextInt(100) >= prob) {
                    log.info("Click target skipped (probability {}%): '{}'", prob, target.text());
                    continue;
                }

                // Replay navigation steps to reach the target
                if (target.navigationSteps() != null && !target.navigationSteps().isEmpty()) {
                    for (NavigationStepDto step : target.navigationSteps()) {
                        String urlBefore = page.url();
                        boolean clicked = clickElementInPageOrFrames(page, step.selector());
                        if (!clicked) {
                            log.warn("Navigation step element not found: '{}' ({})", step.text(), step.selector());
                            continue;
                        }

                        page.waitForTimeout(step.waitAfterMs() > 0 ? step.waitAfterMs() : 2000);

                        // If the page URL changed, wait for the new page + iframes to load
                        String urlAfter = page.url();
                        if (!urlBefore.equals(urlAfter)) {
                            try {
                                page.waitForLoadState(LoadState.DOMCONTENTLOADED,
                                    new Page.WaitForLoadStateOptions().setTimeout(10000));
                            } catch (Exception ignored) {}
                            // Wait for child iframes to load their content
                            for (Frame frame : page.frames()) {
                                if (frame.equals(page.mainFrame())) continue;
                                try {
                                    frame.waitForLoadState(LoadState.DOMCONTENTLOADED,
                                        new Frame.WaitForLoadStateOptions().setTimeout(5000));
                                } catch (Exception ignored) {}
                            }
                            page.waitForTimeout(2000);
                            log.info("Navigation step: clicked '{}' → navigated to {}", step.text(),
                                urlAfter.substring(0, Math.min(80, urlAfter.length())));
                        } else {
                            log.info("Navigation step: clicked '{}' (same page)", step.text());
                        }
                        currentUrl = page.url();
                    }
                } else if (target.pagePath() != null && !target.pagePath().isEmpty() && !target.pagePath().equals("home")) {
                    String targetUrl = baseUrl.replaceAll("/+$", "") + target.pagePath();
                    if (!currentUrl.equals(targetUrl) && !target.pagePath().startsWith("http")) {
                        page.navigate(targetUrl, new Page.NavigateOptions()
                            .setTimeout(15000)
                            .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                        page.waitForTimeout(2000);
                        currentUrl = targetUrl;
                    }
                }

                // Click the target element
                String urlBefore = page.url();
                boolean clicked = clickElementInPageOrFrames(page, target.selector());
                if (!clicked) {
                    log.warn("Click target not found: '{}' ({})", target.text(), target.selector());
                    page.waitForTimeout(target.delayAfterMs() > 0 ? target.delayAfterMs() : 2000);
                    continue;
                }

                page.waitForTimeout(target.delayAfterMs() > 0 ? target.delayAfterMs() : 2000);

                String urlAfter = page.url();
                if (!urlBefore.equals(urlAfter)) {
                    try {
                        page.waitForLoadState(LoadState.DOMCONTENTLOADED,
                            new Page.WaitForLoadStateOptions().setTimeout(10000));
                    } catch (Exception ignored) {}
                    log.info("Click target executed: '{}' → page navigated to {}", target.text(),
                        urlAfter.substring(0, Math.min(80, urlAfter.length())));
                } else {
                    log.info("Click target executed: '{}' (page stayed)", target.text());
                }
                currentUrl = page.url();
            } catch (Exception e) {
                log.warn("Click target failed: '{}' - {}", target.text(), e.getMessage());
            }
        }
    }

    private boolean clickElementInPageOrFrames(Page page, String selector) {
        // Log current frames for debugging
        List<Frame> frames = page.frames();
        if (frames.size() > 1) {
            for (Frame f : frames) {
                if (!f.equals(page.mainFrame())) {
                    log.info("Frame available: {}", f.url().substring(0, Math.min(60, f.url().length())));
                }
            }
        }

        try {
            Locator mainLocator = page.locator(selector).first();
            if (mainLocator.count() > 0) {
                mainLocator.scrollIntoViewIfNeeded(new Locator.ScrollIntoViewIfNeededOptions().setTimeout(5000));
                page.waitForTimeout(500);
                moveMouseTrusted(page);
                mainLocator.click(new Locator.ClickOptions().setTimeout(5000));
                return true;
            }
        } catch (Exception ignored) {}

        // For iframe elements, strip >> nth= suffix and try first match
        String baseSelector = selector.contains(" >> nth=")
            ? selector.substring(0, selector.indexOf(" >> nth="))
            : selector;

        for (Frame frame : page.frames()) {
            if (frame.equals(page.mainFrame())) continue;
            try {
                // Try exact selector first, then base selector as fallback
                for (String sel : new String[]{selector, baseSelector}) {
                    try {
                        Locator frameLocator = frame.locator(sel).first();
                        if (frameLocator.count() > 0) {
                            frameLocator.scrollIntoViewIfNeeded(new Locator.ScrollIntoViewIfNeededOptions().setTimeout(5000));
                            page.waitForTimeout(500);
                            frameLocator.click(new Locator.ClickOptions().setTimeout(5000));
                            return true;
                        }
                    } catch (Exception ignored2) {}
                }
            } catch (Exception ignored) {}
        }
        return false;
    }

    private void clickRandomLink(Page page, String baseUrl) {
        try {
            Object result = page.evaluate("() => {"
                + "const links = document.querySelectorAll('a[href]');"
                + "const internal = Array.from(links)"
                + "  .filter(a => a.href.startsWith(window.location.origin))"
                + "  .filter(a => !a.href.includes('#'))"
                + "  .filter(a => a.offsetParent !== null);"
                + "if (internal.length === 0) return null;"
                + "const link = internal[Math.floor(Math.random() * internal.length)];"
                + "return link.href;"
                + "}");

            if (result instanceof String url && !url.isBlank()) {
                page.navigate(url, new Page.NavigateOptions()
                    .setTimeout(20000)
                    .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                page.waitForTimeout(2000);
                scrollDown(page);
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

    private void acceptCookieConsent(Page page) {
        try {
            try {
                page.waitForSelector(
                    "#onetrust-accept-btn-handler, " +
                    ".cc-btn, " +
                    "#CybotCookiebotDialogBodyButtonAccept, " +
                    "[class*='consent'] button, " +
                    "[class*='cookie'] button",
                    new Page.WaitForSelectorOptions().setTimeout(3000)
                );
            } catch (Exception ignored) {}

            String[] selectors = {
                "#onetrust-accept-btn-handler",
                ".cc-btn.cc-allow",
                ".cc-btn.cc-dismiss",
                "#CybotCookiebotDialogBodyButtonAccept",
                "button[aria-label*='accept' i]",
                "button[aria-label*='agree' i]",
                "[class*='cookie'] button[class*='accept' i]",
                "[class*='consent'] button[class*='accept' i]"
            };

            for (String selector : selectors) {
                try {
                    ElementHandle el = page.querySelector(selector);
                    if (el != null && el.isVisible()) {
                        el.click(new ElementHandle.ClickOptions().setTimeout(3000));
                        page.waitForTimeout(500);
                        return;
                    }
                } catch (Exception ignored) {}
            }

            try {
                ElementHandle el = page.querySelector(
                    "::-p-text(Accept All), ::-p-text(Accept all), " +
                    "::-p-text(Agree), ::-p-text(I Accept), " +
                    "::-p-text(Allow All), ::-p-text(OK)"
                );
                if (el != null && el.isVisible()) {
                    el.click(new ElementHandle.ClickOptions().setTimeout(3000));
                    page.waitForTimeout(500);
                }
            } catch (Exception ignored) {}
        } catch (Exception ignored) {}
    }

    private static String countryLocale(String countryCode, OrganicProfile profile) {
        return switch (countryCode.toUpperCase()) {
            case "DE" -> "de-DE";
            case "FR" -> "fr-FR";
            case "ES" -> "es-ES";
            case "IT" -> "it-IT";
            case "NL" -> "nl-NL";
            case "PL" -> "pl-PL";
            case "BR", "PT" -> "pt-BR";
            case "JP" -> "ja-JP";
            case "KR" -> "ko-KR";
            case "GB" -> "en-GB";
            case "AU" -> "en-AU";
            case "CA" -> "en-CA";
            default -> profile.acceptLanguage().split(",")[0].trim();
        };
    }

    private static String countryAcceptLanguage(String countryCode, OrganicProfile profile) {
        String locale = countryLocale(countryCode, profile);
        String base = locale.split("-")[0];
        return locale + "," + base + ";q=0.9";
    }

    private static String countryTimezone(String countryCode, OrganicProfile profile) {
        return switch (countryCode.toUpperCase()) {
            case "DE" -> "Europe/Berlin";
            case "FR" -> "Europe/Paris";
            case "ES" -> "Europe/Madrid";
            case "IT" -> "Europe/Rome";
            case "NL" -> "Europe/Amsterdam";
            case "PL" -> "Europe/Warsaw";
            case "GB" -> "Europe/London";
            case "AU" -> "Australia/Sydney";
            case "JP" -> "Asia/Tokyo";
            case "KR" -> "Asia/Seoul";
            case "BR" -> "America/Sao_Paulo";
            case "CA" -> "America/Toronto";
            default -> profile.timezone();
        };
    }

    private static final Map<String, double[]> COUNTRY_GEO = Map.ofEntries(
        Map.entry("DE", new double[]{52.5200, 13.4050}),
        Map.entry("FR", new double[]{48.8566, 2.3522}),
        Map.entry("ES", new double[]{40.4168, -3.7038}),
        Map.entry("IT", new double[]{41.9028, 12.4964}),
        Map.entry("NL", new double[]{52.3676, 4.9041}),
        Map.entry("PL", new double[]{52.2297, 21.0122}),
        Map.entry("GB", new double[]{51.5074, -0.1278}),
        Map.entry("AU", new double[]{-33.8688, 151.2093}),
        Map.entry("JP", new double[]{35.6762, 139.6503}),
        Map.entry("KR", new double[]{37.5665, 126.9780}),
        Map.entry("BR", new double[]{-23.5505, -46.6333}),
        Map.entry("CA", new double[]{43.6532, -79.3832})
    );

    private static Geolocation countryGeo(String countryCode, OrganicProfile profile) {
        double[] geo = COUNTRY_GEO.get(countryCode.toUpperCase());
        if (geo != null) return new Geolocation(geo[0], geo[1]);
        return new Geolocation(40.7128, -74.0060);
    }

    public record DiscoveredElement(
        String selector, String text, String tag, String href,
        int x, int y, int width, int height
    ) {}

    public record PageState(
        String id,
        String label,
        String screenshot,
        List<DiscoveredElement> elements,
        int pageWidth,
        int pageHeight
    ) {}

    public record SitePreview(
        List<PageState> states
    ) {}

    private static final String ELEMENT_DISCOVERY_JS = """
        () => {
          const clickable = document.querySelectorAll('a[href], button, [role="button"], input[type="submit"], input[type="button"], [onclick], [data-cy], [data-testid]');
          const scrollY = window.scrollY;
          const scrollX = window.scrollX;
          const raw = [];
          
          clickable.forEach((el) => {
            const rect = el.getBoundingClientRect();
            if (rect.width < 5 || rect.height < 5) return;
            
            const absY = Math.round(rect.top + scrollY);
            const absX = Math.round(rect.left + scrollX);
            
            let selector = '';
            if (el.id && /^[a-zA-Z][\\w-]*$/.test(el.id)) {
              selector = '#' + el.id;
            } else if (el.getAttribute('data-testid')) {
              selector = '[data-testid="' + el.getAttribute('data-testid') + '"]';
            } else if (el.getAttribute('data-cy')) {
              selector = '[data-cy="' + el.getAttribute('data-cy') + '"]';
            } else if (el.getAttribute('aria-label')) {
              selector = '[aria-label="' + el.getAttribute('aria-label') + '"]';
            } else if (el.tagName === 'A' && el.getAttribute('href')) {
              const href = el.getAttribute('href');
              if (href.length > 0 && href.length < 100) {
                selector = 'a[href="' + href + '"]';
              }
            }
            if (!selector) {
              const text = (el.textContent || '').trim();
              if (text.length > 0 && text.length < 50) {
                selector = el.tagName.toLowerCase() + ':has-text("' + text.replace(/"/g, '\\\\"') + '")';
              } else {
                return;
              }
            }
            
            const text = (el.textContent || el.getAttribute('aria-label') || el.getAttribute('value') || '').trim().substring(0, 60);
            if (!text && el.tagName !== 'INPUT') return;
            
            raw.push({
              selector: selector,
              text: text || '(no text)',
              tag: el.tagName.toLowerCase(),
              href: el.getAttribute('href') || '',
              x: absX,
              y: absY,
              width: Math.round(rect.width),
              height: Math.round(rect.height)
            });
          });
          
          const counts = {};
          raw.forEach(r => { counts[r.selector] = (counts[r.selector] || 0) + 1; });
          const indices = {};
          raw.forEach(r => {
            if (counts[r.selector] > 1) {
              const idx = indices[r.selector] || 0;
              indices[r.selector] = idx + 1;
              r.selector = r.selector + ' >> nth=' + idx;
            }
          });
          
          return raw;
        }
        """;

    @SuppressWarnings("unchecked")
    private List<DiscoveredElement> extractElements(Page page) {
        List<DiscoveredElement> elements = new ArrayList<>();

        Object result = page.evaluate(ELEMENT_DISCOVERY_JS);
        if (result instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> map) {
                    elements.add(toDiscoveredElement(map));
                }
            }
        }

        for (Frame frame : page.frames()) {
            if (frame.equals(page.mainFrame())) continue;
            try {
                String frameUrl = frame.url();
                if (frameUrl == null || frameUrl.isEmpty() || frameUrl.equals("about:blank")) continue;

                int offsetX = 0, offsetY = 0;
                try {
                    ElementHandle frameEl = frame.frameElement();
                    if (frameEl != null) {
                        var bb = frameEl.boundingBox();
                        if (bb != null) { offsetX = (int) bb.x; offsetY = (int) bb.y; }
                    }
                } catch (Exception ignored) {}

                Object frameResult = frame.evaluate(ELEMENT_DISCOVERY_JS);
                if (frameResult instanceof List<?> frameList) {
                    for (Object item : frameList) {
                        if (item instanceof Map<?, ?> map) {
                            var el = toDiscoveredElement(map);
                            elements.add(new DiscoveredElement(
                                el.selector(),
                                "[iframe] " + el.text(),
                                el.tag(),
                                el.href(),
                                el.x() + offsetX,
                                el.y() + offsetY,
                                el.width(),
                                el.height()
                            ));
                        }
                    }
                    log.info("Found {} elements in frame: {}", frameList.size(), frameUrl.substring(0, Math.min(50, frameUrl.length())));
                }
            } catch (Exception e) {
                log.debug("Frame element extraction failed: {}", e.getMessage());
            }
        }

        return elements;
    }

    private DiscoveredElement toDiscoveredElement(Map<?, ?> map) {
        return new DiscoveredElement(
            String.valueOf(map.get("selector")),
            String.valueOf(map.get("text")),
            String.valueOf(map.get("tag")),
            String.valueOf(map.get("href")),
            (int) map.get("x"),
            (int) map.get("y"),
            (int) map.get("width"),
            (int) map.get("height")
        );
    }

    private void scrollThroughPage(Page page) {
        try {
            page.evaluate("() => window.scrollTo(0, 0)");
            page.waitForTimeout(500);

            int viewportHeight = (int) page.evaluate("() => window.innerHeight");

            for (int i = 0; i < 15; i++) {
                int target = (int)(viewportHeight * 0.8 * (i + 1));
                page.evaluate("(p) => window.scrollTo(0, p)", target);
                page.waitForTimeout(300);
            }

            page.evaluate("() => window.scrollTo(0, 0)");
            page.waitForTimeout(1000);

            for (Frame frame : page.frames()) {
                if (frame.equals(page.mainFrame())) continue;
                try {
                    frame.waitForLoadState(LoadState.DOMCONTENTLOADED,
                        new Frame.WaitForLoadStateOptions().setTimeout(4000));
                } catch (Exception ignored) {}
            }
            page.waitForTimeout(500);
        } catch (Exception ignored) {}
    }

    private PageState captureState(Page page, String id, String label) {
        scrollThroughPage(page);

        byte[] screenshot = page.screenshot(new Page.ScreenshotOptions()
            .setFullPage(true)
            .setType(ScreenshotType.PNG));
        String screenshotBase64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(screenshot);

        List<DiscoveredElement> elements = extractElements(page);
        int pageWidth = (int) page.evaluate("() => document.documentElement.scrollWidth");
        int pageHeight = (int) page.evaluate("() => document.body.scrollHeight");

        return new PageState(id, label, screenshotBase64, elements, pageWidth, pageHeight);
    }

    public SitePreview discoverElements(String url) {
        getPlaywright();

        BrowserContext ctx = sharedBrowser.newContext(
            new Browser.NewContextOptions().setViewportSize(1280, 720)
        );
        Page page = ctx.newPage();
        List<PageState> states = new ArrayList<>();

        try {
            page.navigate(url, new Page.NavigateOptions()
                .setTimeout(20000)
                .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
            page.waitForTimeout(3000);

            PageState homeState = captureState(page, "home", "Home");
            states.add(homeState);

            @SuppressWarnings("unchecked")
            List<String> navPaths = (List<String>) page.evaluate("""
                () => {
                  const baseUrl = window.location.origin;
                  const links = document.querySelectorAll('nav a[href], header a[href], .nav a[href]');
                  const paths = new Set();
                  links.forEach(a => {
                    const href = a.getAttribute('href');
                    if (href && href.startsWith('/') && !href.startsWith('//') && href.length > 1 && href.length < 50) {
                      paths.add(href);
                    }
                  });
                  return Array.from(paths).slice(0, 5);
                }
                """);

            for (String path : navPaths) {
                try {
                    String fullUrl = url.replaceAll("/+$", "") + path;
                    page.navigate(fullUrl, new Page.NavigateOptions()
                        .setTimeout(15000)
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
                    page.waitForTimeout(3000);

                    String label = path.replaceAll("^/|/$", "");
                    if (label.isEmpty()) label = path;
                    label = label.substring(0, 1).toUpperCase() + (label.length() > 1 ? label.substring(1) : "");

                    states.add(captureState(page, path, label));
                } catch (Exception e) {
                    log.warn("Failed to scan page: {}", path);
                }
            }
        } finally {
            try { page.close(); } catch (Exception ignored) {}
            try { ctx.close(); } catch (Exception ignored) {}
        }

        return new SitePreview(states);
    }

    // ==================== INTERACTIVE SESSION ====================

    private record InteractiveSession(
        String id,
        BrowserContext ctx,
        Page page,
        List<NavigationStepDto> path
    ) {}

    private final Map<String, InteractiveSession> sessions = new ConcurrentHashMap<>();
    private final java.util.concurrent.ScheduledExecutorService sessionCleanup =
        java.util.concurrent.Executors.newScheduledThreadPool(1, Thread.ofVirtual().factory());

    public SessionState startSession(String url) {
        getPlaywright();

        BrowserContext ctx = sharedBrowser.newContext(
            new Browser.NewContextOptions().setViewportSize(1280, 720)
        );
        Page page = ctx.newPage();
        String sessionId = "session-" + System.currentTimeMillis();

        page.navigate(url, new Page.NavigateOptions()
            .setTimeout(20000)
            .setWaitUntil(WaitUntilState.DOMCONTENTLOADED));
        page.waitForTimeout(2000);

        InteractiveSession session = new InteractiveSession(sessionId, ctx, page, new ArrayList<>());
        sessions.put(sessionId, session);

        sessionCleanup.schedule(() -> closeSession(sessionId), 5, TimeUnit.MINUTES);

        return captureSessionState(session);
    }

    public SessionState sessionClick(String sessionId, String selector, String text) {
        InteractiveSession session = sessions.get(sessionId);
        if (session == null) throw new IllegalArgumentException("Session expired");

        try {
            Locator locator = session.page().locator(selector).first();
            if (locator.count() > 0) {
                locator.click(new Locator.ClickOptions().setTimeout(10000));
                session.page().waitForTimeout(2000);
            }
        } catch (Exception e) {
            log.warn("Session click failed: {}", e.getMessage());
        }

        List<NavigationStepDto> newPath = new ArrayList<>(session.path());
        newPath.add(new NavigationStepDto(selector, text, 2000));

        InteractiveSession updated = new InteractiveSession(session.id(), session.ctx(), session.page(), newPath);
        sessions.put(sessionId, updated);

        return captureSessionState(updated);
    }

    public SessionState sessionBack(String sessionId) {
        InteractiveSession session = sessions.get(sessionId);
        if (session == null) throw new IllegalArgumentException("Session expired");

        try {
            session.page().goBack(new Page.GoBackOptions().setTimeout(10000));
            session.page().waitForTimeout(2000);
        } catch (Exception e) {
            log.warn("Session back failed: {}", e.getMessage());
        }

        List<NavigationStepDto> newPath = new ArrayList<>(session.path());
        if (!newPath.isEmpty()) newPath.remove(newPath.size() - 1);

        InteractiveSession updated = new InteractiveSession(session.id(), session.ctx(), session.page(), newPath);
        sessions.put(sessionId, updated);

        return captureSessionState(updated);
    }

    public void closeSession(String sessionId) {
        InteractiveSession session = sessions.remove(sessionId);
        if (session != null) {
            try { session.page().close(); } catch (Exception ignored) {}
            try { session.ctx().close(); } catch (Exception ignored) {}
        }
    }

    private SessionState captureSessionState(InteractiveSession session) {
        Page page = session.page();
        scrollThroughPage(page);

        byte[] screenshot = page.screenshot(new Page.ScreenshotOptions()
            .setFullPage(true)
            .setType(ScreenshotType.PNG));
        String screenshotBase64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(screenshot);

        List<DiscoveredElement> elements = extractElements(page);
        int pageWidth = (int) page.evaluate("() => document.documentElement.scrollWidth");
        int pageHeight = (int) page.evaluate("() => document.body.scrollHeight");
        String currentUrl = page.url();

        return new SessionState(session.id(), screenshotBase64, elements, currentUrl,
            session.path(), pageWidth, pageHeight);
    }

    public void warmUp() {
        log.info("Pre-warming Playwright browser...");
        try {
            getPlaywright();
            log.info("Playwright ready");
        } catch (Exception e) {
            log.error("Playwright warm-up failed", e);
        }
    }

    public void shutdown() {
        if (sharedBrowser != null) {
            try { sharedBrowser.close(); } catch (Exception ignored) {}
            sharedBrowser = null;
        }
        if (playwright != null) {
            try { playwright.close(); } catch (Exception ignored) {}
            playwright = null;
        }
        log.info("Playwright shut down");
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
