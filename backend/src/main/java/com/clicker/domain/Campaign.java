package com.clicker.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.*;

@Entity
@Table(name = "campaigns")
public class Campaign {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "site_id", nullable = false)
    private UUID siteId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignStatus status = CampaignStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "simulation_level", nullable = false)
    private SimulationLevel simulationLevel = SimulationLevel.HTTP_ONLY;

    @Enumerated(EnumType.STRING)
    @Column(name = "traffic_pattern", nullable = false)
    private TrafficPattern trafficPattern = TrafficPattern.CONSTANT;

    @Column(name = "visits_per_hour", nullable = false)
    private int visitsPerHour = 100;

    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes = 60;

    @Column(name = "schedule_cron")
    private String scheduleCron;

    @Column(name = "geo_distribution", columnDefinition = "jsonb")
    private String geoDistribution = "[]";

    @Column(name = "device_profile", columnDefinition = "jsonb")
    private String deviceProfile = "[]";

    @Column(name = "user_agent_config", columnDefinition = "jsonb")
    private String userAgentConfig = "{\"rotation\": \"RANDOM\"}";

    @Column(name = "proxy_config", columnDefinition = "jsonb")
    private String proxyConfig = "{\"provider\": \"ASOCKS\"}";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "last_run_at")
    private Instant lastRunAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", insertable = false, updatable = false)
    private Site site;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("startedAt DESC")
    private List<CampaignRun> runs = new ArrayList<>();

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<CampaignScenario> campaignScenarios = new HashSet<>();

    public Campaign() {
    }

    public Campaign(UUID id, UUID userId, UUID siteId, String name, CampaignStatus status,
                    SimulationLevel simulationLevel, TrafficPattern trafficPattern,
                    int visitsPerHour, int durationMinutes, String scheduleCron,
                    String geoDistribution, String deviceProfile, String userAgentConfig,
                    String proxyConfig, Instant createdAt, Instant updatedAt,
                    Instant lastRunAt) {
        this.id = id;
        this.userId = userId;
        this.siteId = siteId;
        this.name = name;
        this.status = status;
        this.simulationLevel = simulationLevel;
        this.trafficPattern = trafficPattern;
        this.visitsPerHour = visitsPerHour;
        this.durationMinutes = durationMinutes;
        this.scheduleCron = scheduleCron;
        this.geoDistribution = geoDistribution;
        this.deviceProfile = deviceProfile;
        this.userAgentConfig = userAgentConfig;
        this.proxyConfig = proxyConfig;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.lastRunAt = lastRunAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public UUID getSiteId() {
        return siteId;
    }

    public void setSiteId(UUID siteId) {
        this.siteId = siteId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CampaignStatus getStatus() {
        return status;
    }

    public void setStatus(CampaignStatus status) {
        this.status = status;
    }

    public SimulationLevel getSimulationLevel() {
        return simulationLevel;
    }

    public void setSimulationLevel(SimulationLevel simulationLevel) {
        this.simulationLevel = simulationLevel;
    }

    public TrafficPattern getTrafficPattern() {
        return trafficPattern;
    }

    public void setTrafficPattern(TrafficPattern trafficPattern) {
        this.trafficPattern = trafficPattern;
    }

    public int getVisitsPerHour() {
        return visitsPerHour;
    }

    public void setVisitsPerHour(int visitsPerHour) {
        this.visitsPerHour = visitsPerHour;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public String getScheduleCron() {
        return scheduleCron;
    }

    public void setScheduleCron(String scheduleCron) {
        this.scheduleCron = scheduleCron;
    }

    public String getGeoDistribution() {
        return geoDistribution;
    }

    public void setGeoDistribution(String geoDistribution) {
        this.geoDistribution = geoDistribution;
    }

    public String getDeviceProfile() {
        return deviceProfile;
    }

    public void setDeviceProfile(String deviceProfile) {
        this.deviceProfile = deviceProfile;
    }

    public String getUserAgentConfig() {
        return userAgentConfig;
    }

    public void setUserAgentConfig(String userAgentConfig) {
        this.userAgentConfig = userAgentConfig;
    }

    public String getProxyConfig() {
        return proxyConfig;
    }

    public void setProxyConfig(String proxyConfig) {
        this.proxyConfig = proxyConfig;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getLastRunAt() {
        return lastRunAt;
    }

    public void setLastRunAt(Instant lastRunAt) {
        this.lastRunAt = lastRunAt;
    }

    public Site getSite() {
        return site;
    }

    public void setSite(Site site) {
        this.site = site;
    }

    public List<CampaignRun> getRuns() {
        return runs;
    }

    public void setRuns(List<CampaignRun> runs) {
        this.runs = runs;
    }

    public Set<CampaignScenario> getCampaignScenarios() {
        return campaignScenarios;
    }

    public void setCampaignScenarios(Set<CampaignScenario> campaignScenarios) {
        this.campaignScenarios = campaignScenarios;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID userId;
        private UUID siteId;
        private String name;
        private CampaignStatus status = CampaignStatus.DRAFT;
        private SimulationLevel simulationLevel = SimulationLevel.HTTP_ONLY;
        private TrafficPattern trafficPattern = TrafficPattern.CONSTANT;
        private int visitsPerHour = 100;
        private int durationMinutes = 60;
        private String scheduleCron;
        private String geoDistribution = "[]";
        private String deviceProfile = "[]";
        private String userAgentConfig = "{\"rotation\": \"RANDOM\"}";
        private String proxyConfig = "{\"provider\": \"ASOCKS\"}";
        private Instant createdAt = Instant.now();
        private Instant updatedAt = Instant.now();
        private Instant lastRunAt;
        private Site site;
        private List<CampaignRun> runs = new ArrayList<>();
        private Set<CampaignScenario> campaignScenarios = new HashSet<>();

        public Builder() {
        }

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder userId(UUID userId) {
            this.userId = userId;
            return this;
        }

        public Builder siteId(UUID siteId) {
            this.siteId = siteId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder status(CampaignStatus status) {
            this.status = status;
            return this;
        }

        public Builder simulationLevel(SimulationLevel simulationLevel) {
            this.simulationLevel = simulationLevel;
            return this;
        }

        public Builder trafficPattern(TrafficPattern trafficPattern) {
            this.trafficPattern = trafficPattern;
            return this;
        }

        public Builder visitsPerHour(int visitsPerHour) {
            this.visitsPerHour = visitsPerHour;
            return this;
        }

        public Builder durationMinutes(int durationMinutes) {
            this.durationMinutes = durationMinutes;
            return this;
        }

        public Builder scheduleCron(String scheduleCron) {
            this.scheduleCron = scheduleCron;
            return this;
        }

        public Builder geoDistribution(String geoDistribution) {
            this.geoDistribution = geoDistribution;
            return this;
        }

        public Builder deviceProfile(String deviceProfile) {
            this.deviceProfile = deviceProfile;
            return this;
        }

        public Builder userAgentConfig(String userAgentConfig) {
            this.userAgentConfig = userAgentConfig;
            return this;
        }

        public Builder proxyConfig(String proxyConfig) {
            this.proxyConfig = proxyConfig;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public Builder updatedAt(Instant updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }

        public Builder lastRunAt(Instant lastRunAt) {
            this.lastRunAt = lastRunAt;
            return this;
        }

        public Builder site(Site site) {
            this.site = site;
            return this;
        }

        public Builder runs(List<CampaignRun> runs) {
            this.runs = runs;
            return this;
        }

        public Builder campaignScenarios(Set<CampaignScenario> campaignScenarios) {
            this.campaignScenarios = campaignScenarios;
            return this;
        }

        public Campaign build() {
            Campaign campaign = new Campaign();
            campaign.id = this.id;
            campaign.userId = this.userId;
            campaign.siteId = this.siteId;
            campaign.name = this.name;
            campaign.status = this.status;
            campaign.simulationLevel = this.simulationLevel;
            campaign.trafficPattern = this.trafficPattern;
            campaign.visitsPerHour = this.visitsPerHour;
            campaign.durationMinutes = this.durationMinutes;
            campaign.scheduleCron = this.scheduleCron;
            campaign.geoDistribution = this.geoDistribution;
            campaign.deviceProfile = this.deviceProfile;
            campaign.userAgentConfig = this.userAgentConfig;
            campaign.proxyConfig = this.proxyConfig;
            campaign.createdAt = this.createdAt;
            campaign.updatedAt = this.updatedAt;
            campaign.lastRunAt = this.lastRunAt;
            campaign.site = this.site;
            campaign.runs = this.runs;
            campaign.campaignScenarios = this.campaignScenarios;
            return campaign;
        }
    }
}
