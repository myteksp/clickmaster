package com.clicker.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "visit_events")
public class VisitEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "campaign_run_id", nullable = false)
    private UUID campaignRunId;

    @Column(name = "proxy_address")
    private String proxyAddress;

    @Column(name = "country_code", length = 2)
    private String countryCode;

    @Column
    private String path;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "response_time_ms")
    private Integer responseTimeMs;

    @Column(nullable = false)
    private boolean success;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public VisitEvent() {
    }

    public VisitEvent(UUID id, UUID campaignRunId, String proxyAddress, String countryCode,
                      String path, Integer statusCode, Integer responseTimeMs,
                      boolean success, String errorMessage, Instant createdAt) {
        this.id = id;
        this.campaignRunId = campaignRunId;
        this.proxyAddress = proxyAddress;
        this.countryCode = countryCode;
        this.path = path;
        this.statusCode = statusCode;
        this.responseTimeMs = responseTimeMs;
        this.success = success;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCampaignRunId() {
        return campaignRunId;
    }

    public void setCampaignRunId(UUID campaignRunId) {
        this.campaignRunId = campaignRunId;
    }

    public String getProxyAddress() {
        return proxyAddress;
    }

    public void setProxyAddress(String proxyAddress) {
        this.proxyAddress = proxyAddress;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    public Integer getResponseTimeMs() {
        return responseTimeMs;
    }

    public void setResponseTimeMs(Integer responseTimeMs) {
        this.responseTimeMs = responseTimeMs;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private UUID id;
        private UUID campaignRunId;
        private String proxyAddress;
        private String countryCode;
        private String path;
        private Integer statusCode;
        private Integer responseTimeMs;
        private boolean success;
        private String errorMessage;
        private Instant createdAt = Instant.now();

        public Builder() {
        }

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder campaignRunId(UUID campaignRunId) {
            this.campaignRunId = campaignRunId;
            return this;
        }

        public Builder proxyAddress(String proxyAddress) {
            this.proxyAddress = proxyAddress;
            return this;
        }

        public Builder countryCode(String countryCode) {
            this.countryCode = countryCode;
            return this;
        }

        public Builder path(String path) {
            this.path = path;
            return this;
        }

        public Builder statusCode(Integer statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder responseTimeMs(Integer responseTimeMs) {
            this.responseTimeMs = responseTimeMs;
            return this;
        }

        public Builder success(boolean success) {
            this.success = success;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public VisitEvent build() {
            VisitEvent ve = new VisitEvent();
            ve.id = this.id;
            ve.campaignRunId = this.campaignRunId;
            ve.proxyAddress = this.proxyAddress;
            ve.countryCode = this.countryCode;
            ve.path = this.path;
            ve.statusCode = this.statusCode;
            ve.responseTimeMs = this.responseTimeMs;
            ve.success = this.success;
            ve.errorMessage = this.errorMessage;
            ve.createdAt = this.createdAt;
            return ve;
        }
    }
}
