package com.contractiq.service;

import com.contractiq.dao.ActivityDao;
import com.contractiq.dao.ContractDao;
import com.contractiq.dao.ObligationDao;
import com.contractiq.dto.DashboardStatsDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public class DashboardService {
    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);

    private final ContractDao contractDao;
    private final ObligationDao obligationDao;
    private final ActivityDao activityDao;

    public DashboardService() {
        this.contractDao = new ContractDao();
        this.obligationDao = new ObligationDao();
        this.activityDao = new ActivityDao();
    }

    public DashboardService(ContractDao contractDao, ObligationDao obligationDao, ActivityDao activityDao) {
        this.contractDao = contractDao;
        this.obligationDao = obligationDao;
        this.activityDao = activityDao;
    }

    public DashboardStatsDto getDashboardStats() throws SQLException {
        DashboardStatsDto stats = new DashboardStatsDto();

        stats.setTotalContracts(contractDao.countTotal());
        stats.setPendingReviewContracts(contractDao.countPendingReview());
        stats.setApprovedObligations(obligationDao.countApproved());
        stats.setPendingObligations(obligationDao.countPending());
        stats.setUpcomingRenewalsCount(contractDao.countUpcomingRenewals());

        stats.setContractsByType(contractDao.countByType());
        stats.setObligationsByCategory(obligationDao.countByCategory());
        stats.setObligationsByStatus(obligationDao.countByStatus());

        stats.setUpcomingRenewals(contractDao.findUpcomingRenewals(6));
        stats.setRecentActivity(activityDao.findRecent(10));

        return stats;
    }
}
