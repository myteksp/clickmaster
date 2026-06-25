package com.clicker.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "campaign_runs")
public class CampaignRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "campaign_id", nullable = false)
    private UUID campaignId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RunStatus status = RunStatus.RUNNING;

    @Column(name = "started_at", nullable = false, updatable = false)
    private Instant startedAt = Instant.now();

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "total_visits", nullable = false)
    private int totalVisits = 0;

    @Column(name = "successful_visits", nullable = false)
    private int successfulVisits = 0;

    @Column(name = "failed_visits", nullable = false)
    private int failedVisits = 0;

    @Column(name = "error_log", columnDefinition = "jsonb")
    private String errorLog;

    @Column(columnDefinition = "jsonb")
    private String stats;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", insertable = false, updatable = false)
    private Campaign campaign;

    public CampaignRun() {
    }

    public CampaignRun(UUID id, UUID campaignId, RunStatus status, Instant startedAt,
                       Instant finishedAt, int totalVisits, int successfulVisits,
                       int failedVisits, String errorLog, String stats) {
        this.id = id;
        this.campaignId = campaignId;
        this.status = status;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.totalVisits = totalVisits;
        this.successfulVisits = successfulVisits;
        this.failedVisits = failedVisits;
        this.errorLog = errorLog;
        this.stats = stats;
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

    public RunStatus getStatus() {
        return status;
    }

    public void setStatus(RunStatus status) {
        this.status = status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public int getTotalVisits() {
        return totalVisits;
    }

    public void setTotalVisits(int totalVisits) {
        this.totalVisits = totalVisits;
    }

    public int getSuccessfulVisits() {
        return successfulVisits;
    }

    public void setSuccessfulVisits(int successfulVisits) {
        this.successfulVisits = successfulVisits;
    }

    public int getFailedVisits() {
        return failedVisits;
    }

    public void setFailedVisits(int failedVisits) {
        this.failedVisits = failedVisits;
    }

    public String getErrorLog() {
        return errorLog;
    }

    public void setErrorLog(String errorLog) {
        this.errorLog = errorLog;
    }

    public String getStats() {
        return stats;
    }

    public void setStats(String stats) {
        this.stats = stats;
    }

    public Campaign getCampaign() {
        return campaign;
    }

    public void setCampaign(Campaign campaign) {
        this.campaign = campaign;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID campaignId;
        private RunStatus status = RunStatus.RUNNING;
        private Instant startedAt = Instant.now();
        private Instant finishedAt;
        private int totalVisits = 0;
        private int successfulVisits = 0;
        private int failedVisits = 0;
        private String errorLog;
        private String stats;
        private Campaign campaign;

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

        public Builder status(RunStatus status) {
            this.status = status;
            return this;
        }

        public Builder startedAt(Instant startedAt) {
            this.startedAt = startedAt;
            return this;
        }

        public Builder finishedAt(Instant finishedAt) {
            this.finishedAt = finishedAt;
            return this;
        }

        public Builder totalVisits(int totalVisits) {
            this.totalVisits = totalVisits;
            return this;
        }

        public Builder successfulVisits(int successfulVisits) {
            this.successfulVisits = successfulVisits;
            return this;
        }

        public Builder failedVisits(int failedVisits) {
            this.failedVisits = failedVisits;
            return this;
        }

        public Builder errorLog(String errorLog) {
            this.errorLog = errorLog;
            return this;
        }

        public Builder stats(String stats) {
            this.stats = stats;
            return this;
        }

        public Builder campaign(Campaign campaign) {
            this.campaign = campaign;
            return this;
        }

        public CampaignRun build() {
            CampaignRun run = new CampaignRun();
            run.id = this.id;
            run.campaignId = this.campaignId;
            run.status = this.status;
            run.startedAt = this.startedAt;
            run.finishedAt = this.finishedAt;
            run.totalVisits = this.totalVisits;
            run.successfulVisits = this.successfulVisits;
            run.failedVisits = this.failedVisits;
            run.errorLog = this.errorLog;
            run.stats = this.stats;
            run.campaign = this.campaign;
            return run;
        }
    }
}
