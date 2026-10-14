package com.contractiq.controller;

import com.contractiq.dto.ApiResponse;
import com.contractiq.exception.ResourceNotFoundException;
import com.contractiq.exception.ValidationException;
import com.contractiq.util.JsonUtil;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;

public abstract class BaseServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(BaseServlet.class);

    protected void setCommonHeaders(HttpServletResponse resp) {
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        resp.setHeader("X-Content-Type-Options", "nosniff");
    }

    protected void sendOk(HttpServletResponse resp, Object data) throws IOException {
        setCommonHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok(data)));
    }

    protected void sendOk(HttpServletResponse resp, String message, Object data) throws IOException {
        setCommonHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok(message, data)));
    }

    protected void sendCreated(HttpServletResponse resp, String message, Object data) throws IOException {
        setCommonHeaders(resp);
        resp.setStatus(HttpServletResponse.SC_CREATED);
        resp.getWriter().write(JsonUtil.toJson(ApiResponse.ok(message, data)));
    }

    protected void sendError(HttpServletResponse resp, int statusCode, String message) throws IOException {
        setCommonHeaders(resp);
        resp.setStatus(statusCode);
        resp.getWriter().write(JsonUtil.toJson(ApiResponse.failure(message, message)));
    }

    protected <T> T readJsonBody(HttpServletRequest req, Class<T> clazz) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = req.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        String body = sb.toString();
        if (body.trim().isEmpty()) {
            return null;
        }
        return JsonUtil.fromJson(body, clazz);
    }

    protected void handleException(HttpServletResponse resp, Exception e) throws IOException {
        if (e instanceof ValidationException) {
            logger.warn("Validation error: {}", e.getMessage());
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
        } else if (e instanceof ResourceNotFoundException) {
            logger.warn("Not found: {}", e.getMessage());
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, e.getMessage());
        } else {
            logger.error("Internal server error: {}", e.getMessage(), e);
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "An internal server error occurred: " + e.getMessage());
        }
    }
}
