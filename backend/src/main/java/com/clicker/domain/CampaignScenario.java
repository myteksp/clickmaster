package com.clicker.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "campaign_scenarios")
public class CampaignScenario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Column(name = "scenario_id", nullable = false)
    private UUID scenarioId;

    @Column(name = "entry_url")
    private String entryUrl;

    @Column(nullable = false)
    private int weight = 100;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", insertable = false, updatable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", insertable = false, updatable = false)
    private Scenario scenario;

    public CampaignScenario() {
    }

    public CampaignScenario(UUID id, UUID campaignId, UUID scenarioId, String entryUrl,
                            int weight) {
        this.id = id;
        this.campaignId = campaignId;
        this.scenarioId = scenarioId;
        this.entryUrl = entryUrl;
        this.weight = weight;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(UUID campaignId) {
        this.campaignId = campaignId;
    }

    public UUID getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(UUID scenarioId) {
        this.scenarioId = scenarioId;
    }

    public String getEntryUrl() {
        return entryUrl;
    }

    public void setEntryUrl(String entryUrl) {
        this.entryUrl = entryUrl;
    }

    public int getWeight() {
        return weight;
    }

    public void setWeight(int weight) {
        this.weight = weight;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    public Scenario getScenario() {
        return scenario;
    }

    public void setScenario(Scenario scenario) {
        this.scenario = scenario;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID campaignId;
        private UUID scenarioId;
        private String entryUrl;
        private int weight = 100;
        private Campaign campaign;
        private Scenario scenario;

        public Builder() {
        }

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder campaignId(UUID campaignId) {
            this.campaignId = campaignId;
            return this;
        }

        public Builder scenarioId(UUID scenarioId) {
            this.scenarioId = scenarioId;
            return this;
        }

        public Builder entryUrl(String entryUrl) {
            this.entryUrl = entryUrl;
            return this;
        }

        public Builder weight(int weight) {
            this.weight = weight;
            return this;
        }

        public Builder campaign(Campaign campaign) {
            this.campaign = campaign;
            return this;
        }

        public Builder scenario(Scenario scenario) {
            this.scenario = scenario;
            return this;
        }

        public CampaignScenario build() {
            CampaignScenario cs = new CampaignScenario();
            cs.id = this.id;
            cs.campaignId = this.campaignId;
            cs.scenarioId = this.scenarioId;
            cs.entryUrl = this.entryUrl;
            cs.weight = this.weight;
            cs.campaign = this.campaign;
            cs.scenario = this.scenario;
            return cs;
        }
    }
}
