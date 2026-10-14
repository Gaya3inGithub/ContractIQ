package com.contractiq.config;

import com.contractiq.util.EnvLoader;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private static final String[] REQUIRED_TABLES = {"contracts", "obligations", "activity_logs"};
    private static volatile HikariDataSource dataSource;
    private static DataSource customDataSource; // Used by unit tests (e.g. H2)
    private static volatile boolean schemaInitializationCompleted = false;

    private DatabaseConfig() {}

    public static synchronized void setCustomDataSource(DataSource ds) {
        customDataSource = ds;
        schemaInitializationCompleted = false;
    }

    public static synchronized DataSource getDataSource() {
        if (customDataSource != null) {
            return customDataSource;
        }

        if (dataSource == null) {
            String dbUrl = EnvLoader.get("DB_URL", "jdbc:mysql://localhost:3306/contractiq_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC&characterEncoding=UTF-8");
            String dbUser = EnvLoader.get("DB_USER", "root");
            String dbPassword = EnvLoader.get("DB_PASSWORD", "");
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

        initializeSchemaIfMissing();
        return dataSource;
    }

    public static synchronized void initializeSchemaIfMissing() {
        if (schemaInitializationCompleted) {
            return;
        }

        DataSource source = customDataSource != null ? customDataSource : dataSource;
        if (source == null) {
            throw new IllegalStateException("Database source is not initialized.");
        }

        try (Connection conn = source.getConnection()) {
            if (hasRequiredTables(conn)) {
                schemaInitializationCompleted = true;
                logger.info("Database schema already initialized for catalog '{}' and required tables are present.", conn.getCatalog());
                return;
            }

            String sqlScript = loadSchemaSqlScript();
            List<String> statements = splitSqlStatements(sqlScript);
            if (statements.isEmpty()) {
                throw new IllegalStateException("No database schema statements were found in src/main/resources/schema.sql.");
            }

            logger.info("Required tables are missing in catalog '{}'; initializing database schema.", conn.getCatalog());
            try (Statement statement = conn.createStatement()) {
                for (String sql : statements) {
                    String trimmed = sql.trim();
                    if (trimmed.isEmpty() || trimmed.startsWith("--")) {
                        continue;
                    }
                    statement.execute(trimmed);
                }
            }

            if (!hasRequiredTables(conn)) {
                throw new IllegalStateException("Database schema initialization completed but required tables are still missing.");
            }

            schemaInitializationCompleted = true;
            logger.info("Database schema initialization completed successfully for catalog '{}'.", conn.getCatalog());
        } catch (Exception e) {
            logger.error("Database schema initialization failed: {}", e.getMessage(), e);
            throw new IllegalStateException("ContractIQ database schema initialization failed. Check the configured MySQL database and schema script.", e);
        }
    }

    private static boolean hasRequiredTables(Connection conn) throws SQLException {
        Set<String> tableNames = new HashSet<>();
        DatabaseMetaData metaData = conn.getMetaData();
        try (ResultSet rs = metaData.getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {
            while (rs.next()) {
                String tableName = rs.getString("TABLE_NAME");
                if (tableName != null) {
                    tableNames.add(tableName.toLowerCase(Locale.ROOT));
                }
            }
        }

        for (String requiredTable : REQUIRED_TABLES) {
            if (!tableNames.contains(requiredTable)) {
                return false;
            }
        }
        return true;
    }

    private static String loadSchemaSqlScript() {
        String resourceName = "schema.sql";
        try (InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(resourceName)) {
            if (inputStream == null) {
                throw new IllegalStateException("Unable to load schema resource 'schema.sql' from the classpath.");
            }
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read database schema script from classpath.", e);
        }
    }

    private static List<String> splitSqlStatements(String script) {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String rawLine : script.split("\\R")) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("--")) {
                continue;
            }
            if (line.startsWith("USE ") || line.startsWith("CREATE DATABASE")) {
                continue;
            }
            current.append(line).append(System.lineSeparator());
            if (line.endsWith(";")) {
                String statement = normalizeStatement(current.toString());
                if (!statement.isEmpty()) {
                    statements.add(statement);
                }
                current.setLength(0);
            }
        }
        if (current.length() > 0) {
            String trailing = normalizeStatement(current.toString());
            if (!trailing.isEmpty()) {
                statements.add(trailing);
            }
        }
        return statements;
    }

    private static String normalizeStatement(String statement) {
        String cleaned = statement.trim();
        if (cleaned.isEmpty()) {
            return "";
        }

        cleaned = cleaned.replaceAll("(?i)\\s+ENGINE\\s*=\\s*InnoDB", "");
        cleaned = cleaned.replaceAll("(?i)\\s+DEFAULT\\s+CHARSET\\s*=\\s*utf8mb4", "");
        cleaned = cleaned.replaceAll("(?i)\\s+DEFAULT\\s+CHARSET\\s*=?\\s*utf8mb4", "");
        cleaned = cleaned.replaceAll("(?i)\\s+CHARSET\\s*=\\s*utf8mb4", "");
        cleaned = cleaned.replaceAll("(?i)\\s+CHARACTER\\s+SET\\s+utf8mb4", "");
        cleaned = cleaned.replaceAll("(?i)\\s+COLLATE\\s*=?\\s*utf8mb4_unicode_ci", "");
        cleaned = cleaned.replaceAll("(?i)\\s+COLLATE\\s+utf8mb4_unicode_ci", "");
        cleaned = cleaned.replaceAll("(?i)\\s+DEFAULT\\s+CHARSET\\s*=?\\s*utf8mb4\\s+COLLATE\\s*=?\\s*utf8mb4_unicode_ci", "");
        cleaned = cleaned.replaceAll("(?i)\\s+CHARACTER\\s+SET\\s+utf8mb4\\s+COLLATE\\s+utf8mb4_unicode_ci", "");

        cleaned = cleaned.replaceAll("\\s{2,}", " ").trim();

        if (cleaned.endsWith(";")) {
            return cleaned;
        }
        return cleaned + ";";
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
        customDataSource = null;
        schemaInitializationCompleted = false;
    }
}
