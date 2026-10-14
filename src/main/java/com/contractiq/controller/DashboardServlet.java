package com.contractiq.controller;

import com.contractiq.dto.DashboardStatsDto;
import com.contractiq.service.DashboardService;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

@WebServlet(name = "DashboardServlet", urlPatterns = {"/api/dashboard/stats"})
public class DashboardServlet extends BaseServlet {

    private final DashboardService dashboardService;

    public DashboardServlet() {
        this.dashboardService = new DashboardService();
    }

    public DashboardServlet(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            DashboardStatsDto stats = dashboardService.getDashboardStats();
            sendOk(resp, stats);
        } catch (Exception e) {
            handleException(resp, e);
        }
    }
}
