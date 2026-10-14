package com.contractiq.config;

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.WebListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@WebListener
public class AppContextListener implements ServletContextListener {
    private static final Logger logger = LoggerFactory.getLogger(AppContextListener.class);

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        logger.info("================================================================================");
        logger.info("Initializing {} v{}", AppConfig.APP_NAME, AppConfig.APP_VERSION);
        logger.info("Upload storage is configured.");
        logger.info("LLM Provider configured: [Configured: {}, Model: {}]",
                AppConfig.isLlmConfigured(), AppConfig.getLlmModel());

        try {
            boolean dbOk = DatabaseConfig.testConnection();
            if (!dbOk) {
                throw new IllegalStateException("Database connection could not be established at startup. Please verify DB credentials.");
            }
            DatabaseConfig.initializeSchemaIfMissing();
            logger.info("Database connection established successfully and schema is initialized.");
        } catch (Exception e) {
            logger.error("Error during startup database verification or schema initialization: {}", e.getMessage(), e);
            throw new IllegalStateException("ContractIQ database startup validation failed.", e);
        }
        logger.info("{} initialized and ready to serve requests.", AppConfig.APP_NAME);
        logger.info("================================================================================");
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        logger.info("Shutting down {}...", AppConfig.APP_NAME);
        DatabaseConfig.shutdown();
        logger.info("{} shutdown completed.", AppConfig.APP_NAME);
    }
}
