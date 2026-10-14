package com.contractiq.controller;

import com.contractiq.model.Contract;
import com.contractiq.exception.ValidationException;
import com.contractiq.service.ContractService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "RenewalServlet", urlPatterns = {"/api/renewals"})
public class RenewalServlet extends BaseServlet {

    private final ContractService contractService;

    public RenewalServlet() {
        this.contractService = new ContractService();
    }

    public RenewalServlet(ContractService contractService) {
        this.contractService = contractService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            int limit = 50;
            String limitParam = req.getParameter("limit");
            if (limitParam != null) {
                try {
                    limit = Integer.parseInt(limitParam.trim());
                } catch (NumberFormatException ignored) {
                    throw new ValidationException("Renewal limit must be a whole number.");
                }
            }
            if (limit < 1 || limit > 100) {
                throw new ValidationException("Renewal limit must be between 1 and 100.");
            }

            List<Contract> renewals = contractService.getUpcomingRenewals(limit);
            sendOk(resp, renewals);
        } catch (Exception e) {
            handleException(resp, e);
        }
    }
}
