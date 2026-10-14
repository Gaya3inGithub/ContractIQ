package com.contractiq.controller;

import com.contractiq.exception.ValidationException;
import com.contractiq.model.Contract;
import com.contractiq.model.Obligation;
import com.contractiq.service.ContractService;
import com.contractiq.service.ObligationService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "ContractServlet", urlPatterns = {"/api/contracts", "/api/contracts/*"})
public class ContractServlet extends BaseServlet {

    private final ContractService contractService;
    private final ObligationService obligationService;

    public ContractServlet() {
        this.contractService = new ContractService();
        this.obligationService = new ObligationService();
    }

    public ContractServlet(ContractService contractService, ObligationService obligationService) {
        this.contractService = contractService;
        this.obligationService = obligationService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();

        try {
            // Case 1: GET /api/contracts (pathInfo is null or "/")
            if (pathInfo == null || pathInfo.equals("/") || pathInfo.isEmpty()) {
                String status = req.getParameter("status");
                String search = req.getParameter("search");
                List<Contract> contracts = contractService.listContracts(status, search);
                sendOk(resp, contracts);
                return;
            }

            // Parse path: /{id} or /{id}/obligations
            String[] parts = pathInfo.split("/");
            if (parts.length >= 2) {
                Long contractId = parseId(parts[1]);

                // Case 2: GET /api/contracts/{id}/obligations
                if (parts.length >= 3 && "obligations".equalsIgnoreCase(parts[2])) {
                    List<Obligation> obligations = obligationService.getObligationsForContract(contractId);
                    sendOk(resp, obligations);
                    return;
                }

                // Case 3: GET /api/contracts/{id}
                Contract contract = contractService.getContract(contractId);
                sendOk(resp, contract);
                return;
            }

            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Invalid contract route: " + pathInfo);

        } catch (Exception e) {
            handleException(resp, e);
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        if (pathInfo == null || pathInfo.length() <= 1) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "Contract ID is required in URL path.");
            return;
        }

        try {
            String[] parts = pathInfo.split("/");
            Long contractId = parseId(parts[1]);
            boolean deleted = contractService.deleteContract(contractId);
            if (deleted) {
                sendOk(resp, "Contract deleted successfully.", null);
            } else {
                sendError(resp, HttpServletResponse.SC_NOT_FOUND, "Contract not found.");
            }
        } catch (Exception e) {
            handleException(resp, e);
        }
    }

    private Long parseId(String idStr) {
        try {
            return Long.parseLong(idStr.trim());
        } catch (NumberFormatException e) {
            throw new ValidationException("Invalid contract ID: '" + idStr + "'. Must be numeric.");
        }
    }
}
