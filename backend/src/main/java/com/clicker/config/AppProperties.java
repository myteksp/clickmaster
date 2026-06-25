package com.clicker.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app")
public class AppProperties {

    private Jwt jwt = new Jwt();
    private Asocks asocks = new Asocks();
    private Simulation simulation = new Simulation();

    public Jwt getJwt() {
        return jwt;
    }

    public void setJwt(Jwt jwt) {
        this.jwt = jwt;
    }

    public Asocks getAsocks() {
        return asocks;
    }

    public void setAsocks(Asocks asocks) {
        this.asocks = asocks;
    }

    public Simulation getSimulation() {
        return simulation;
    }

    public void setSimulation(Simulation simulation) {
        this.simulation = simulation;
    }

    public static class Jwt {
        private String secret;
        private long expirationMs;

        public String getSecret() {
            return secret;
        }

        public void setSecret(String secret) {
            this.secret = secret;
        }

        public long getExpirationMs() {
            return expirationMs;
        }

        public void setExpirationMs(long expirationMs) {
            this.expirationMs = expirationMs;
        }
    }

    public static class Asocks {
        private String baseUrl;
        private String apiKey;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }

    public static class Simulation {
        private Playwright playwright = new Playwright();
        private Http http = new Http();
        private int proxyRefreshIntervalMinutes;
        private String userAgentPoolFile;

        public Playwright getPlaywright() {
            return playwright;
        }

        public void setPlaywright(Playwright playwright) {
            this.playwright = playwright;
        }

        public Http getHttp() {
            return http;
        }

        public void setHttp(Http http) {
            this.http = http;
        }

        public int getProxyRefreshIntervalMinutes() {
            return proxyRefreshIntervalMinutes;
        }

        public void setProxyRefreshIntervalMinutes(int proxyRefreshIntervalMinutes) {
            this.proxyRefreshIntervalMinutes = proxyRefreshIntervalMinutes;
        }

        public String getUserAgentPoolFile() {
            return userAgentPoolFile;
        }

        public void setUserAgentPoolFile(String userAgentPoolFile) {
            this.userAgentPoolFile = userAgentPoolFile;
        }

        public static class Playwright {
            private boolean headless = true;
            private int maxConcurrentBrowsers = 10;
            private int browserPoolSize = 5;

            public boolean isHeadless() {
                return headless;
            }

            public void setHeadless(boolean headless) {
                this.headless = headless;
            }

            public int getMaxConcurrentBrowsers() {
                return maxConcurrentBrowsers;
            }

            public void setMaxConcurrentBrowsers(int maxConcurrentBrowsers) {
                this.maxConcurrentBrowsers = maxConcurrentBrowsers;
            }

            public int getBrowserPoolSize() {
                return browserPoolSize;
            }

            public void setBrowserPoolSize(int browserPoolSize) {
                this.browserPoolSize = browserPoolSize;
            }
        }

        public static class Http {
            private int maxConcurrentRequests = 50;
            private int requestTimeoutSeconds = 30;

            public int getMaxConcurrentRequests() {
                return maxConcurrentRequests;
            }

            public void setMaxConcurrentRequests(int maxConcurrentRequests) {
                this.maxConcurrentRequests = maxConcurrentRequests;
            }

            public int getRequestTimeoutSeconds() {
                return requestTimeoutSeconds;
            }

            public void setRequestTimeoutSeconds(int requestTimeoutSeconds) {
                this.requestTimeoutSeconds = requestTimeoutSeconds;
            }
        }
    }
}
