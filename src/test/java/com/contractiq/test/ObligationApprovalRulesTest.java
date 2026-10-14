package com.contractiq.test;

import com.contractiq.dao.ActivityDao;
import com.contractiq.dao.ContractDao;
import com.contractiq.dao.ObligationDao;
import com.contractiq.dto.ApprovalDecisionRequest;
import com.contractiq.dto.ObligationUpdateRequest;
import com.contractiq.model.Contract;
import com.contractiq.model.Obligation;
import com.contractiq.service.ObligationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ObligationApprovalRulesTest {

    @Mock
    private ObligationDao obligationDao;

    @Mock
    private ContractDao contractDao;

    @Mock
    private ActivityDao activityDao;

    private ObligationService obligationService;

    @BeforeEach
    void setUp() {
        obligationService = new ObligationService(obligationDao, contractDao, activityDao);
    }

    @Test
    void testObligationInitialStatusIsPending() {
        Obligation o = new Obligation();
        assertEquals("PENDING", o.getApprovalStatus(), "AI extracted obligations must initialize to PENDING");
    }

    @Test
    void testApproveObligationEnforcesHumanDecision() throws SQLException {
        Obligation ob = new Obligation();
        ob.setId(10L);
        ob.setContractId(1L);
        ob.setObligationTitle("Pay Platform Fees");
        ob.setApprovalStatus("PENDING");

        when(obligationDao.findById(10L)).thenReturn(Optional.of(ob));
        when(obligationDao.findByContractId(1L)).thenReturn(List.of(ob));

        ApprovalDecisionRequest req = new ApprovalDecisionRequest("Senior Legal Reviewer", "Audited against schedule A");
        Obligation approved = obligationService.approveObligation(10L, req);

        assertNotNull(approved);
        assertEquals("APPROVED", approved.getApprovalStatus());
        assertEquals("Senior Legal Reviewer", approved.getApprovedBy());
        assertEquals("Audited against schedule A", approved.getReviewComment());

        verify(obligationDao).updateApprovalStatus(eq(10L), eq("APPROVED"), eq("Senior Legal Reviewer"), eq("Audited against schedule A"));
        verify(contractDao).updateReviewStatus(eq(1L), eq("APPROVED"), any());
        verify(activityDao).log(any());
    }

    @Test
    void testRejectObligationEnforcesHumanDecision() throws SQLException {
        Obligation ob = new Obligation();
        ob.setId(20L);
        ob.setContractId(2L);
        ob.setObligationTitle("Penalty clause");
        ob.setApprovalStatus("PENDING");

        when(obligationDao.findById(20L)).thenReturn(Optional.of(ob));
        when(obligationDao.findByContractId(2L)).thenReturn(List.of(ob));

        ApprovalDecisionRequest req = new ApprovalDecisionRequest("Compliance Officer", "Non-standard clause rejected");
        Obligation rejected = obligationService.rejectObligation(20L, req);

        assertNotNull(rejected);
        assertEquals("REJECTED", rejected.getApprovalStatus());
        assertEquals("Compliance Officer", rejected.getApprovedBy());

        verify(obligationDao).updateApprovalStatus(eq(20L), eq("REJECTED"), eq("Compliance Officer"), eq("Non-standard clause rejected"));
        verify(contractDao).updateReviewStatus(eq(2L), eq("REJECTED"), any());
    }

    @Test
    void testEditObligationBeforeApproval() throws SQLException {
        Obligation ob = new Obligation();
        ob.setId(30L);
        ob.setContractId(3L);
        ob.setObligationTitle("Old Title");
        ob.setDescription("Old Description");
        ob.setApprovalStatus("PENDING");

        when(obligationDao.findById(30L)).thenReturn(Optional.of(ob));

        ObligationUpdateRequest req = new ObligationUpdateRequest();
        req.setObligationTitle("Corrected Legal Title");
        req.setDescription("Updated terms following addendum review");

        Obligation updated = obligationService.updateObligation(30L, req);

        assertEquals("Corrected Legal Title", updated.getObligationTitle());
        assertEquals("Updated terms following addendum review", updated.getDescription());
        assertEquals("PENDING", updated.getApprovalStatus(), "Editing does not bypass pending approval status");

        verify(obligationDao).update(ob);
        verify(activityDao).log(any());
    }
}
