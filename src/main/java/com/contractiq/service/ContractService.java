package com.contractiq.service;

import com.contractiq.dao.ActivityDao;
import com.contractiq.dao.ContractDao;
import com.contractiq.dao.ObligationDao;
import com.contractiq.exception.ResourceNotFoundException;
import com.contractiq.model.ActivityLog;
import com.contractiq.model.Contract;
import com.contractiq.model.Obligation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.List;

public class ContractService {
    private static final Logger logger = LoggerFactory.getLogger(ContractService.class);

    private final ContractDao contractDao;
    private final ObligationDao obligationDao;
    private final ActivityDao activityDao;

    public ContractService() {
        this.contractDao = new ContractDao();
        this.obligationDao = new ObligationDao();
        this.activityDao = new ActivityDao();
    }

    public ContractService(ContractDao contractDao, ObligationDao obligationDao, ActivityDao activityDao) {
        this.contractDao = contractDao;
        this.obligationDao = obligationDao;
        this.activityDao = activityDao;
    }

    public List<Contract> listContracts(String statusFilter, String search) throws SQLException {
        return contractDao.findAll(statusFilter, search);
    }

    public Contract getContract(Long id) throws SQLException {
        Contract contract = contractDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contract with ID " + id + " not found."));
        List<Obligation> obligations = obligationDao.findByContractId(id);
        contract.setObligations(obligations);
        return contract;
    }

    public List<Contract> getUpcomingRenewals(int limit) throws SQLException {
        return contractDao.findUpcomingRenewals(limit);
    }

    public boolean deleteContract(Long id) throws SQLException {
        Contract c = contractDao.findById(id).orElse(null);
        boolean deleted = contractDao.delete(id);
        if (deleted && c != null) {
            activityDao.log(new ActivityLog(null, "CONTRACT_DELETED",
                    "Contract #" + id + " ('" + c.getContractTitle() + "') was deleted.", "System"));
        }
        return deleted;
    }
}
