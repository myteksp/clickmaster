package com.clicker.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UserAgentServiceTest {

    private final UserAgentService service = new UserAgentService();

    @Test
    void shouldReturnRandomUserAgent() {
        service.init();

        String ua = service.getRandom();
        assertThat(ua).isNotBlank();
        assertThat(ua).contains("Mozilla");
    }

    @Test
    void shouldReturnDesktopChromeAgent() {
        service.init();

        String ua = service.getForDeviceProfile("desktop", "chrome");
        assertThat(ua).contains("Chrome");
        assertThat(ua).doesNotContain("Mobile");
        assertThat(ua).doesNotContain("iPhone");
    }

    @Test
    void shouldReturnMobileAgent() {
        service.init();

        String ua = service.getForDeviceProfile("mobile", "safari");
        assertThat(ua).contains("iPhone");
    }

    @Test
    void shouldReturnAllUserAgents() {
        service.init();

        var all = service.getAll();
        assertThat(all).isNotEmpty();
        assertThat(all).allMatch(ua -> ua.startsWith("Mozilla"));
    }

    @Test
    void shouldReturnDifferentAgentsOnMultipleCalls() {
        service.init();
        var all = service.getAll();
        var samples = new java.util.HashSet<String>();

        for (int i = 0; i < all.size() * 3; i++) {
            samples.add(service.getRandom());
        }
        // With reasonable randomness, should get more than 1 unique
        assertThat(samples.size()).isGreaterThan(1);
    }
}
