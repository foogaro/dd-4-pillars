package com.foogaro.hotel.common.model;

import java.time.Instant;

public class WorkflowStep {

    private String name;
    private WorkflowState state;
    private String outcome;
    private String detail;
    private Instant at;

    public WorkflowStep() {
    }

    public WorkflowStep(String name, WorkflowState state, String outcome, String detail) {
        this.name = name;
        this.state = state;
        this.outcome = outcome;
        this.detail = detail;
        this.at = Instant.now();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public WorkflowState getState() {
        return state;
    }

    public void setState(WorkflowState state) {
        this.state = state;
    }

    public String getOutcome() {
        return outcome;
    }

    public void setOutcome(String outcome) {
        this.outcome = outcome;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public Instant getAt() {
        return at;
    }

    public void setAt(Instant at) {
        this.at = at;
    }
}
