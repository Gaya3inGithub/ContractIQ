package com.contractiq.model;

import java.time.LocalDateTime;

public class ActivityLog {
    private Long id;
    private Long contractId;
    private String actionType;
    private String description;
    private String actor = "System";
    private String ipAddress;
    private LocalDateTime createdAt;

    public ActivityLog() {}

    public ActivityLog(Long contractId, String actionType, String description, String actor) {
        this(contractId, actionType, description, actor, null);
    }

    public ActivityLog(Long contractId, String actionType, String description, String actor, String ipAddress) {
        this.contractId = contractId;
        this.actionType = actionType;
        this.description = description;
        this.actor = actor != null ? actor : "System";
        this.ipAddress = ipAddress;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getContractId() { return contractId; }
    public void setContractId(Long contractId) { this.contractId = contractId; }

    public String getActionType() { return actionType; }
    public void setActionType(String actionType) { this.actionType = actionType; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getActor() { return actor; }
    public void setActor(String actor) { this.actor = actor; }

    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
