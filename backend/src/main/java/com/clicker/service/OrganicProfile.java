package com.clicker.service;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public record OrganicProfile(
    String id,
    String device,
    String os,
    String browser,
    String browserVersion,
    BrowserEngine engine,
    String userAgent,
    List<String> accept,
    String acceptLanguage,
    String acceptEncoding,
    String secChUa,
    String secChUaMobile,
    String secChUaPlatform,
    int viewportWidth,
    int viewportHeight,
    int devicePixelRatio,
    String platform,
    String timezone,
    List<String> languages,
    List<String> fonts,
    String webglVendor,
    String webglRenderer
) {
    public enum BrowserEngine {
        BLINK,     // Chrome, Edge, Opera, Brave
        GECKO,     // Firefox
        WEBKIT     // Safari
    }

    private static final List<String> CHROME_ACCEPT = List.of(
        "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"
    );

    private static final List<String> FIREFOX_ACCEPT = List.of(
        "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8"
    );

    private static final List<String> SAFARI_ACCEPT = List.of(
        "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"
    );

    public static final List<OrganicProfile> DESKTOP_PROFILES = List.of(
        new OrganicProfile(
            "chrome-win", "desktop", "Windows 10", "Chrome", "131",
            BrowserEngine.BLINK,
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
            CHROME_ACCEPT, "en-US,en;q=0.9", "gzip, deflate, br",
            "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"",
            "?0", "\"Windows\"",
            1920, 1080, 1, "Win32", "America/New_York",
            List.of("en-US", "en"), List.of("Arial", "Times New Roman", "Courier New"),
            "Google Inc. (NVIDIA)", "ANGLE (NVIDIA, NVIDIA GeForce RTX 3060 Direct3D11 vs_5_0 ps_5_0)"
        ),
        new OrganicProfile(
            "chrome-mac", "desktop", "macOS", "Chrome", "131",
            BrowserEngine.BLINK,
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
            CHROME_ACCEPT, "en-US,en;q=0.9", "gzip, deflate, br",
            "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"",
            "?0", "\"macOS\"",
            1680, 1050, 2, "MacIntel", "America/Los_Angeles",
            List.of("en-US", "en"), List.of("Arial", "Helvetica", "Times", "Courier"),
            "Apple", "Apple M1"
        ),
        new OrganicProfile(
            "chrome-linux", "desktop", "Linux", "Chrome", "131",
            BrowserEngine.BLINK,
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36",
            CHROME_ACCEPT, "en-US,en;q=0.9", "gzip, deflate, br",
            "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"",
            "?0", "\"Linux\"",
            1920, 1080, 1, "Linux x86_64", "Europe/London",
            List.of("en-US", "en"), List.of("DejaVu Sans", "Liberation Sans"),
            "Google Inc. (Intel)", "ANGLE (Intel, Mesa Intel(R) Graphics (ADL GT2))"
        ),
        new OrganicProfile(
            "firefox-win", "desktop", "Windows 10", "Firefox", "133",
            BrowserEngine.GECKO,
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:133.0) Gecko/20100101 Firefox/133.0",
            FIREFOX_ACCEPT, "en-US,en;q=0.5", "gzip, deflate, br",
            null, null, null,
            1920, 1080, 1, "Win32", "America/Chicago",
            List.of("en-US", "en"), List.of("Arial", "Times New Roman"),
            null, null
        ),
        new OrganicProfile(
            "firefox-mac", "desktop", "macOS", "Firefox", "133",
            BrowserEngine.GECKO,
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10.15; rv:133.0) Gecko/20100101 Firefox/133.0",
            FIREFOX_ACCEPT, "en-US,en;q=0.5", "gzip, deflate, br",
            null, null, null,
            1680, 1050, 2, "MacIntel", "America/Denver",
            List.of("en-US", "en"), List.of("Arial", "Helvetica", "Times"),
            null, null
        ),
        new OrganicProfile(
            "safari-mac", "desktop", "macOS", "Safari", "18.2",
            BrowserEngine.WEBKIT,
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.2 Safari/605.1.15",
            SAFARI_ACCEPT, "en-US,en;q=0.9", "gzip, deflate, br",
            null, null, null,
            1680, 1050, 2, "MacIntel", "America/Los_Angeles",
            List.of("en-US", "en"), List.of("Helvetica", "Times", "Courier"),
            "Apple", "Apple M1"
        ),
        new OrganicProfile(
            "edge-win", "desktop", "Windows 10", "Edge", "131",
            BrowserEngine.BLINK,
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.0.0 Safari/537.36 Edg/131.0.0.0",
            CHROME_ACCEPT, "en-US,en;q=0.9", "gzip, deflate, br",
            "\"Microsoft Edge\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"",
            "?0", "\"Windows\"",
            1920, 1080, 1, "Win32", "America/New_York",
            List.of("en-US", "en"), List.of("Arial", "Times New Roman"),
            "Google Inc. (NVIDIA)", "ANGLE (NVIDIA, NVIDIA GeForce RTX 3060 Direct3D11 vs_5_0 ps_5_0)"
        )
    );

    public static final List<OrganicProfile> MOBILE_PROFILES = List.of(
        new OrganicProfile(
            "chrome-android", "mobile", "Android 14", "Chrome Mobile", "131",
            BrowserEngine.BLINK,
            "Mozilla/5.0 (Linux; Android 14; Pixel 8 Pro) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/131.0.6778.81 Mobile Safari/537.36",
            CHROME_ACCEPT, "en-US,en;q=0.9", "gzip, deflate, br",
            "\"Google Chrome\";v=\"131\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"",
            "?1", "\"Android\"",
            412, 915, 3, "Linux armv8l", "America/New_York",
            List.of("en-US", "en"), List.of("Roboto", "Arial"),
            "Qualcomm", "Adreno (TM) 750"
        ),
        new OrganicProfile(
            "safari-ios", "mobile", "iOS 18.2", "Safari Mobile", "18.2",
            BrowserEngine.WEBKIT,
            "Mozilla/5.0 (iPhone; CPU iPhone OS 18_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/18.2 Mobile/15E148 Safari/604.1",
            SAFARI_ACCEPT, "en-US,en;q=0.9", "gzip, deflate, br",
            null, null, null,
            393, 852, 3, "iPhone", "America/Los_Angeles",
            List.of("en-US", "en"), List.of("Helvetica", "Georgia"),
            "Apple", "Apple A17 Pro"
        ),
        new OrganicProfile(
            "samsung-android", "mobile", "Android 14", "Samsung Browser", "24",
            BrowserEngine.BLINK,
            "Mozilla/5.0 (Linux; Android 14; SM-S928B) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/24.0 Chrome/131.0.6778.81 Mobile Safari/537.36",
            CHROME_ACCEPT, "en-US,en;q=0.9", "gzip, deflate, br",
            "\"Samsung Browser\";v=\"24\", \"Chromium\";v=\"131\", \"Not_A Brand\";v=\"24\"",
            "?1", "\"Android\"",
            384, 854, 3, "Linux armv8l", "Europe/London",
            List.of("en-GB", "en"), List.of("Roboto", "Arial"),
            "Qualcomm", "Adreno (TM) 750"
        )
    );

    public static OrganicProfile randomMatching(String device, String browser) {
        var all = new java.util.ArrayList<OrganicProfile>();
        all.addAll(DESKTOP_PROFILES);
        all.addAll(MOBILE_PROFILES);

        var matching = all.stream()
            .filter(p -> (device == null || device.isBlank() || p.device().equalsIgnoreCase(device))
                      && (browser == null || browser.isBlank() || p.browser().toLowerCase().startsWith(browser.toLowerCase())))
            .toList();

        if (matching.isEmpty()) {
            matching = all.stream()
                .filter(p -> device == null || device.isBlank() || p.device().equalsIgnoreCase(device))
                .toList();
        }

        if (matching.isEmpty()) matching = all;

        return matching.get(ThreadLocalRandom.current().nextInt(matching.size()));
    }

    public static OrganicProfile random() {
        var all = new java.util.ArrayList<OrganicProfile>();
        all.addAll(DESKTOP_PROFILES);
        all.addAll(MOBILE_PROFILES);
        return all.get(ThreadLocalRandom.current().nextInt(all.size()));
    }
}
