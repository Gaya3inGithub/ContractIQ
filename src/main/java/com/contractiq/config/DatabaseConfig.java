package com.contractiq.config;

import com.contractiq.util.EnvLoader;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private static volatile HikariDataSource dataSource;
    private static DataSource customDataSource; // Used by unit tests (e.g. H2)

    private DatabaseConfig() {}

    public static synchronized void setCustomDataSource(DataSource ds) {
        customDataSource = ds;
    }

    public static synchronized DataSource getDataSource() {
        if (customDataSource != null) {
            return customDataSource;
        }

        if (dataSource == null) {
            String dbUrl = EnvLoader.get("DB_URL", "jdbc:mysql://localhost:3306/contractiq_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8");
            String dbUser = EnvLoader.get("DB_USER", "root");
            String dbPassword = EnvLoader.get("DB_PASSWORD", "accord");
            int poolSize = EnvLoader.getInt("DB_POOL_SIZE", 10);

            logger.info("Initializing HikariCP connection pool for: {}", dbUrl);

            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(dbUrl);
            config.setUsername(dbUser);
            config.setPassword(dbPassword);
            config.setMaximumPoolSize(poolSize);
            config.setMinimumIdle(2);
            config.setIdleTimeout(300000);
            config.setConnectionTimeout(10000);
            config.setMaxLifetime(1800000);
            config.setPoolName("ContractIQ-HikariPool");

            // Driver class detection
            if (dbUrl.startsWith("jdbc:mysql:")) {
                config.setDriverClassName("com.mysql.cj.jdbc.Driver");
            } else if (dbUrl.startsWith("jdbc:h2:")) {
                config.setDriverClassName("org.h2.Driver");
            }

            try {
                dataSource = new HikariDataSource(config);
                logger.info("HikariCP connection pool successfully initialized.");
            } catch (Exception e) {
                logger.error("Failed to initialize HikariCP connection pool: {}", e.getMessage(), e);
                throw new RuntimeException("Database pool initialization failed: " + e.getMessage(), e);
            }
        }
        return dataSource;
    }

    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            logger.warn("Database connection test failed: {}", e.getMessage());
            return false;
        }
    }

    public static synchronized void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            logger.info("Shutting down HikariCP connection pool...");
            dataSource.close();
            dataSource = null;
        }
    }
}
