package com.contractiq.dto;

import com.contractiq.model.ActivityLog;
import com.contractiq.model.Contract;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardStatsDto {
    private long totalContracts;
    private long pendingReviewContracts;
    private long approvedObligations;
    private long pendingObligations;
    private long upcomingRenewalsCount;

    private Map<String, Integer> obligationsByCategory = new HashMap<>();
    private Map<String, Integer> contractsByType = new HashMap<>();
    private Map<String, Integer> obligationsByStatus = new HashMap<>();

    private List<Contract> upcomingRenewals;
    private List<ActivityLog> recentActivity;

    public DashboardStatsDto() {}

    public long getTotalContracts() { return totalContracts; }
    public void setTotalContracts(long totalContracts) { this.totalContracts = totalContracts; }

    public long getPendingReviewContracts() { return pendingReviewContracts; }
    public void setPendingReviewContracts(long pendingReviewContracts) { this.pendingReviewContracts = pendingReviewContracts; }

    public long getApprovedObligations() { return approvedObligations; }
    public void setApprovedObligations(long approvedObligations) { this.approvedObligations = approvedObligations; }

    public long getPendingObligations() { return pendingObligations; }
    public void setPendingObligations(long pendingObligations) { this.pendingObligations = pendingObligations; }

    public long getUpcomingRenewalsCount() { return upcomingRenewalsCount; }
    public void setUpcomingRenewalsCount(long upcomingRenewalsCount) { this.upcomingRenewalsCount = upcomingRenewalsCount; }

    public Map<String, Integer> getObligationsByCategory() { return obligationsByCategory; }
    public void setObligationsByCategory(Map<String, Integer> obligationsByCategory) { this.obligationsByCategory = obligationsByCategory; }

    public Map<String, Integer> getContractsByType() { return contractsByType; }
    public void setContractsByType(Map<String, Integer> contractsByType) { this.contractsByType = contractsByType; }

    public Map<String, Integer> getObligationsByStatus() { return obligationsByStatus; }
    public void setObligationsByStatus(Map<String, Integer> obligationsByStatus) { this.obligationsByStatus = obligationsByStatus; }

    public List<Contract> getUpcomingRenewals() { return upcomingRenewals; }
    public void setUpcomingRenewals(List<Contract> upcomingRenewals) { this.upcomingRenewals = upcomingRenewals; }

    public List<ActivityLog> getRecentActivity() { return recentActivity; }
    public void setRecentActivity(List<ActivityLog> recentActivity) { this.recentActivity = recentActivity; }
}
