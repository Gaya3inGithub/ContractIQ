package com.contractiq.controller;

import com.contractiq.dao.ActivityDao;
import com.contractiq.model.ActivityLog;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@WebServlet(name = "ActivityServlet", urlPatterns = {"/api/activity"})
public class ActivityServlet extends BaseServlet {

    private final ActivityDao activityDao;

    public ActivityServlet() {
        this.activityDao = new ActivityDao();
    }

    public ActivityServlet(ActivityDao activityDao) {
        this.activityDao = activityDao;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            int limit = 50;
            String limitParam = req.getParameter("limit");
            if (limitParam != null) {
                try {
                    limit = Integer.parseInt(limitParam.trim());
                } catch (NumberFormatException ignored) {}
            }

            String contractIdParam = req.getParameter("contractId");
            List<ActivityLog> logs;
            if (contractIdParam != null && !contractIdParam.trim().isEmpty()) {
                long cId = Long.parseLong(contractIdParam.trim());
                logs = activityDao.findByContractId(cId);
            } else {
                logs = activityDao.findRecent(limit);
            }

            sendOk(resp, logs);
        } catch (Exception e) {
            handleException(resp, e);
        }
    }
}
