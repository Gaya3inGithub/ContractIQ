package com.contractiq.controller;

import com.contractiq.dto.ApprovalDecisionRequest;
import com.contractiq.dto.ObligationUpdateRequest;
import com.contractiq.exception.ValidationException;
import com.contractiq.model.Obligation;
import com.contractiq.service.ObligationService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "ObligationServlet", urlPatterns = {"/api/obligations", "/api/obligations/*"})
public class ObligationServlet extends BaseServlet {

    private final ObligationService obligationService;

    public ObligationServlet() {
        this.obligationService = new ObligationService();
    }

    public ObligationServlet(ObligationService obligationService) {
        this.obligationService = obligationService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();

        try {
            // Case 1: GET /api/obligations
            if (pathInfo == null || pathInfo.equals("/") || pathInfo.isEmpty()) {
                String status = req.getParameter("status");
                String category = req.getParameter("category");
                String search = req.getParameter("search");
                List<Obligation> obligations = obligationService.listObligations(status, category, search);
                sendOk(resp, obligations);
                return;
            }

            // Case 2: GET /api/obligations/{id}
            String[] parts = pathInfo.split("/");
            if (parts.length >= 2) {
                Long id = parseId(parts[1]);
                Obligation obligation = obligationService.getObligation(id);
                sendOk(resp, obligation);
                return;
            }

            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid obligation route: " + pathInfo);

        } catch (Exception e) {
            handleException(resp, e);
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() <= 1) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Obligation ID is required in URL path.");
            return;
        }

        try {
            String[] parts = pathInfo.split("/");
            Long id = parseId(parts[1]);

            ObligationUpdateRequest updateReq = readJsonBody(req, ObligationUpdateRequest.class);
            if (updateReq == null) {
                throw new ValidationException("Request body cannot be empty for obligation update.");
            }

            Obligation updated = obligationService.updateObligation(id, updateReq);
            sendOk(resp, "Obligation updated successfully.", updated);

        } catch (Exception e) {
            handleException(resp, e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() <= 1) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Obligation ID and action are required in URL path.");
            return;
        }

        try {
            String[] parts = pathInfo.split("/");
            // Expected: /{id}/approve or /{id}/reject
            if (parts.length >= 3) {
                Long id = parseId(parts[1]);
                String action = parts[2].toLowerCase();

                ApprovalDecisionRequest decisionReq = readJsonBody(req, ApprovalDecisionRequest.class);
                if (decisionReq == null) {
                    decisionReq = new ApprovalDecisionRequest("Legal Counsel", "Decision executed.");
                }

                if ("approve".equals(action)) {
                    Obligation approved = obligationService.approveObligation(id, decisionReq);
                    sendOk(resp, "Obligation successfully approved.", approved);
                    return;
                } else if ("reject".equals(action)) {
                    Obligation rejected = obligationService.rejectObligation(id, decisionReq);
                    sendOk(resp, "Obligation marked as rejected.", rejected);
                    return;
                }
            }

            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid obligation action route. Use /api/obligations/{id}/approve or /api/obligations/{id}/reject");

        } catch (Exception e) {
            handleException(resp, e);
        }
    }

    private Long parseId(String idStr) {
        try {
            return Long.parseLong(idStr.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid obligation ID: '" + idStr + "'. Must be numeric.");
        }
    }
}
