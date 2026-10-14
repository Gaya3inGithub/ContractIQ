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
import java.util.Set;

public class ObligationService {
    private static final Logger logger = LoggerFactory.getLogger(ObligationService.class);
    private static final Set<String> ALLOWED_FREQUENCIES = Set.of("ONE_TIME", "MONTHLY", "QUARTERLY", "ANNUALLY", "AS_INCURRED");
    private static final Set<String> ALLOWED_CATEGORIES = Set.of("PAYMENT", "COMPLIANCE", "DELIVERY", "REPORTING", "TERMINATION", "CONFIDENTIALITY", "OTHER");

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
            String submittedDate = req.getDeadlineDate().trim();
            LocalDate deadline = submittedDate.isEmpty() ? null : DateUtil.parseSafe(submittedDate);
            if (!submittedDate.isEmpty() && deadline == null) {
                throw new ValidationException("Deadline date must be an unambiguous, valid date.");
            }
            o.setDeadlineDate(deadline);
        }
        if (req.getFrequency() != null) {
            o.setFrequency(validateEnum(req.getFrequency(), ALLOWED_FREQUENCIES, "frequency"));
        }
        if (req.getCategory() != null) {
            o.setCategory(validateEnum(req.getCategory(), ALLOWED_CATEGORIES, "category"));
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
     * Enforces human review by moving a PENDING suggestion to APPROVED once.
     */
    public Obligation approveObligation(Long id, ApprovalDecisionRequest req) throws SQLException {
        Obligation o = getObligation(id);

        requirePendingDecision(o);
        String reviewer = requireReviewerName(req);
        String comment = (req != null && req.getComment() != null) ? req.getComment().trim() : "Approved by human reviewer.";

        if (!obligationDao.updatePendingApprovalStatus(id, "APPROVED", reviewer, comment)) {
            throw new ValidationException("This obligation has already been reviewed. Refresh the page before taking another action.");
        }
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
     * Moves a PENDING obligation to REJECTED with mandatory reviewer tracking.
     */
    public Obligation rejectObligation(Long id, ApprovalDecisionRequest req) throws SQLException {
        Obligation o = getObligation(id);

        requirePendingDecision(o);
        String reviewer = requireReviewerName(req);
        String comment = (req != null && req.getComment() != null) ? req.getComment().trim() : "Rejected during human review.";

        if (!obligationDao.updatePendingApprovalStatus(id, "REJECTED", reviewer, comment)) {
            throw new ValidationException("This obligation has already been reviewed. Refresh the page before taking another action.");
        }
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

    private void requirePendingDecision(Obligation obligation) {
        if (!"PENDING".equalsIgnoreCase(obligation.getApprovalStatus())) {
            throw new ValidationException("Only pending obligations can be approved or rejected. This decision cannot be changed through the review API.");
        }
    }

    private String requireReviewerName(ApprovalDecisionRequest request) {
        if (request == null || request.getReviewerName() == null || request.getReviewerName().trim().isEmpty()) {
            throw new ValidationException("Reviewer name is required for an approval or rejection decision.");
        }
        return request.getReviewerName().trim();
    }

    private String validateEnum(String value, Set<String> allowedValues, String fieldName) {
        String normalised = value.trim().toUpperCase();
        if (!allowedValues.contains(normalised)) {
            throw new ValidationException("Unsupported obligation " + fieldName + ".");
        }
        return normalised;
    }
}
