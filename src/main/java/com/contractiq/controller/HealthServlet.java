package com.contractiq.controller;

import com.contractiq.config.AppConfig;
import com.contractiq.config.DatabaseConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@WebServlet(name = "HealthServlet", urlPatterns = {"/api/health"})
public class HealthServlet extends BaseServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        boolean dbOk = DatabaseConfig.testConnection();

        Map<String, Object> health = new HashMap<>();
        health.put("status", dbOk ? "UP" : "DEGRADED");
        health.put("application", AppConfig.APP_NAME);
        health.put("version", AppConfig.APP_VERSION);
        health.put("javaVersion", System.getProperty("java.version"));

        Map<String, Object> components = new HashMap<>();
        components.put("database", dbOk ? "CONNECTED" : "DISCONNECTED");
        components.put("llmEngine", AppConfig.isLlmConfigured() ? "LIVE_LLM (" + AppConfig.getLlmModel() + ")" : "OFFLINE_HEURISTIC_MODE");
        components.put("storagePath", AppConfig.getUploadDir());

        health.put("components", components);
        health.put("timestamp", LocalDateTime.now().toString());

        sendOk(resp, health);
    }
}
