package com.contractiq.service;

import com.contractiq.dao.ActivityDao;
import com.contractiq.dao.ContractDao;
import com.contractiq.dao.ObligationDao;
import com.contractiq.dto.ApprovalDecisionRequest;
import com.contractiq.dto.ObligationUpdateRequest;
import com.contractiq.exception.ResourceNotFoundException;
import com.contractiq.exception.ValidationException;
import com.contractiq.model.ActivityLog;
import com.contractiq.model.Contract;
import com.contractiq.model.Obligation;
import com.contractiq.util.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

public class ObligationService {
    private static final Logger logger = LoggerFactory.getLogger(ObligationService.class);

    private final ObligationDao obligationDao;
    private final ContractDao contractDao;
    private final ActivityDao activityDao;

    public ObligationService() {
        this.obligationDao = new ObligationDao();
        this.contractDao = new ContractDao();
        this.activityDao = new ActivityDao();
    }

    public ObligationService(ObligationDao obligationDao, ContractDao contractDao, ActivityDao activityDao) {
        this.obligationDao = obligationDao;
        this.contractDao = contractDao;
        this.activityDao = activityDao;
    }

    public List<Obligation> getObligationsForContract(Long contractId) throws SQLException {
        return obligationDao.findByContractId(contractId);
    }

    public List<Obligation> listObligations(String approvalStatus, String category, String search) throws SQLException {
        return obligationDao.findAll(approvalStatus, category, search);
    }

    public Obligation getObligation(Long id) throws SQLException {
        return obligationDao.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Obligation with ID " + id + " not found."));
    }

    public Obligation updateObligation(Long id, ObligationUpdateRequest req) throws SQLException {
        Obligation o = getObligation(id);

        if (req.getObligationTitle() != null && !req.getObligationTitle().trim().isEmpty()) {
            o.setObligationTitle(req.getObligationTitle().trim());
        }
        if (req.getDescription() != null && !req.getDescription().trim().isEmpty()) {
            o.setDescription(req.getDescription().trim());
        }
        if (req.getResponsibleParty() != null) {
            o.setResponsibleParty(req.getResponsibleParty().trim());
        }
        if (req.getDeadlineDate() != null) {
            o.setDeadlineDate(DateUtil.parseSafe(req.getDeadlineDate()));
        }
        if (req.getFrequency() != null) {
            o.setFrequency(req.getFrequency().trim().toUpperCase());
        }
        if (req.getCategory() != null) {
            o.setCategory(req.getCategory().trim().toUpperCase());
        }
        if (req.getReviewComment() != null) {
            o.setReviewComment(req.getReviewComment().trim());
        }

        obligationDao.update(o);

        activityDao.log(new ActivityLog(o.getContractId(), "OBLIGATION_EDITED",
                "Obligation #" + id + " ('" + o.getObligationTitle() + "') updated by reviewer.", "Legal Reviewer"));

        return o;
    }

    /**
     * MANDATORY HUMAN APPROVAL:
     * Enforces human review by explicitly moving status from PENDING/REJECTED to APPROVED.
     */
    public Obligation approveObligation(Long id, ApprovalDecisionRequest req) throws SQLException {
        Obligation o = getObligation(id);

        String reviewer = (req != null && req.getReviewerName() != null && !req.getReviewerName().trim().isEmpty())
                ? req.getReviewerName().trim() : "Legal Reviewer";
        String comment = (req != null && req.getComment() != null) ? req.getComment().trim() : "Approved by human reviewer.";

        obligationDao.updateApprovalStatus(id, "APPROVED", reviewer, comment);
        o.setApprovalStatus("APPROVED");
        o.setApprovedBy(reviewer);
        o.setReviewComment(comment);

        activityDao.log(new ActivityLog(o.getContractId(), "OBLIGATION_APPROVED",
                "Obligation #" + id + " ('" + o.getObligationTitle() + "') was APPROVED by " + reviewer + ".", reviewer));

        // Re-evaluate parent contract review status
        syncContractReviewStatus(o.getContractId());

        return o;
    }

    /**
     * MANDATORY HUMAN REJECTION:
     * Moves obligation status to REJECTED with mandatory reviewer tracking.
     */
    public Obligation rejectObligation(Long id, ApprovalDecisionRequest req) throws SQLException {
        Obligation o = getObligation(id);

        String reviewer = (req != null && req.getReviewerName() != null && !req.getReviewerName().trim().isEmpty())
                ? req.getReviewerName().trim() : "Legal Reviewer";
        String comment = (req != null && req.getComment() != null) ? req.getComment().trim() : "Rejected during human review.";

        obligationDao.updateApprovalStatus(id, "REJECTED", reviewer, comment);
        o.setApprovalStatus("REJECTED");
        o.setApprovedBy(reviewer);
        o.setReviewComment(comment);

        activityDao.log(new ActivityLog(o.getContractId(), "OBLIGATION_REJECTED",
                "Obligation #" + id + " ('" + o.getObligationTitle() + "') was REJECTED by " + reviewer + ".", reviewer));

        // Re-evaluate parent contract review status
        syncContractReviewStatus(o.getContractId());

        return o;
    }

    /**
     * Synchronizes the parent contract's review status based on its obligations.
     */
    private void syncContractReviewStatus(Long contractId) throws SQLException {
        List<Obligation> obligations = obligationDao.findByContractId(contractId);
        if (obligations.isEmpty()) return;

        long approvedCount = obligations.stream().filter(ob -> "APPROVED".equalsIgnoreCase(ob.getApprovalStatus())).count();
        long rejectedCount = obligations.stream().filter(ob -> "REJECTED".equalsIgnoreCase(ob.getApprovalStatus())).count();
        long pendingCount = obligations.stream().filter(ob -> "PENDING".equalsIgnoreCase(ob.getApprovalStatus())).count();

        String newStatus;
        if (pendingCount == 0 && approvedCount > 0) {
            newStatus = "APPROVED";
        } else if (pendingCount == 0 && rejectedCount == obligations.size()) {
            newStatus = "REJECTED";
        } else if (approvedCount > 0 || rejectedCount > 0) {
            newStatus = "PARTIALLY_APPROVED";
        } else {
            newStatus = "NEEDS_REVIEW";
        }

        String note = String.format("Obligation decisions: %d Approved, %d Rejected, %d Pending", approvedCount, rejectedCount, pendingCount);
        contractDao.updateReviewStatus(contractId, newStatus, note);
    }
}
