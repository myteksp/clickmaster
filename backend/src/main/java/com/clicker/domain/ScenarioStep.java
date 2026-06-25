package com.clicker.domain;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "scenario_steps")
public class ScenarioStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "scenario_id", nullable = false)
    private UUID scenarioId;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false)
    private StepAction actionType;

    @Column
    private String selector;

    @Column(name = "\"value\"", columnDefinition = "TEXT")
    private String value;

    @Column(name = "delay_before_ms", nullable = false)
    private int delayBeforeMs = 0;

    @Column(name = "delay_after_ms", nullable = false)
    private int delayAfterMs = 0;

    @Column(nullable = false)
    private double probability = 1.0;

    @Column(columnDefinition = "jsonb")
    private String config;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scenario_id", insertable = false, updatable = false)
    private Scenario scenario;

    public ScenarioStep() {
    }

    public ScenarioStep(UUID id, UUID scenarioId, int orderIndex, StepAction actionType,
                        String selector, String value, int delayBeforeMs,
                        int delayAfterMs, double probability, String config) {
        this.id = id;
        this.scenarioId = scenarioId;
        this.orderIndex = orderIndex;
        this.actionType = actionType;
        this.selector = selector;
        this.value = value;
        this.delayBeforeMs = delayBeforeMs;
        this.delayAfterMs = delayAfterMs;
        this.probability = probability;
        this.config = config;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getScenarioId() {
        return scenarioId;
    }

    public void setScenarioId(UUID scenarioId) {
        this.scenarioId = scenarioId;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public StepAction getActionType() {
        return actionType;
    }

    public void setActionType(StepAction actionType) {
        this.actionType = actionType;
    }

    public String getSelector() {
        return selector;
    }

    public void setSelector(String selector) {
        this.selector = selector;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public int getDelayBeforeMs() {
        return delayBeforeMs;
    }

    public void setDelayBeforeMs(int delayBeforeMs) {
        this.delayBeforeMs = delayBeforeMs;
    }

    public int getDelayAfterMs() {
        return delayAfterMs;
    }

    public void setDelayAfterMs(int delayAfterMs) {
        this.delayAfterMs = delayAfterMs;
    }

    public double getProbability() {
        return probability;
    }

    public void setProbability(double probability) {
        this.probability = probability;
    }

    public String getConfig() {
        return config;
    }

    public void setConfig(String config) {
        this.config = config;
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
        private UUID scenarioId;
        private int orderIndex;
        private StepAction actionType;
        private String selector;
        private String value;
        private int delayBeforeMs = 0;
        private int delayAfterMs = 0;
        private double probability = 1.0;
        private String config;
        private Scenario scenario;

        public Builder() {
        }

        public Builder id(UUID id) {
            this.id = id;
            return this;
        }

        public Builder scenarioId(UUID scenarioId) {
            this.scenarioId = scenarioId;
            return this;
        }

        public Builder orderIndex(int orderIndex) {
            this.orderIndex = orderIndex;
            return this;
        }

        public Builder actionType(StepAction actionType) {
            this.actionType = actionType;
            return this;
        }

        public Builder selector(String selector) {
            this.selector = selector;
            return this;
        }

        public Builder value(String value) {
            this.value = value;
            return this;
        }

        public Builder delayBeforeMs(int delayBeforeMs) {
            this.delayBeforeMs = delayBeforeMs;
            return this;
        }

        public Builder delayAfterMs(int delayAfterMs) {
            this.delayAfterMs = delayAfterMs;
            return this;
        }

        public Builder probability(double probability) {
            this.probability = probability;
            return this;
        }

        public Builder config(String config) {
            this.config = config;
            return this;
        }

        public Builder scenario(Scenario scenario) {
            this.scenario = scenario;
            return this;
        }

        public ScenarioStep build() {
            ScenarioStep step = new ScenarioStep();
            step.id = this.id;
            step.scenarioId = this.scenarioId;
            step.orderIndex = this.orderIndex;
            step.actionType = this.actionType;
            step.selector = this.selector;
            step.value = this.value;
            step.delayBeforeMs = this.delayBeforeMs;
            step.delayAfterMs = this.delayAfterMs;
            step.probability = this.probability;
            step.config = this.config;
            step.scenario = this.scenario;
            return step;
        }
    }
}
