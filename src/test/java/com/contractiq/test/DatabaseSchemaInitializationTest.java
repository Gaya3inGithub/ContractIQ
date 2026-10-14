package com.contractiq.test;

import com.contractiq.config.DatabaseConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseSchemaInitializationTest {

    @Test
    void initializeSchemaIfMissingIsIdempotentAndPreservesExistingData() throws Exception {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl("jdbc:h2:mem:contractiq_schema_test;MODE=MySQL;DB_CLOSE_DELAY=-1;DATABASE_TO_UPPER=FALSE");
        hikariConfig.setUsername("sa");
        hikariConfig.setPassword("");
        hikariConfig.setMaximumPoolSize(2);
        hikariConfig.setDriverClassName("org.h2.Driver");

        DatabaseConfig.setCustomDataSource(new HikariDataSource(hikariConfig));

        try {
            DatabaseConfig.initializeSchemaIfMissing();

            try (Connection conn = DatabaseConfig.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate("INSERT INTO contracts (file_name, contract_title, review_status, extraction_status, file_size_bytes) VALUES ('keep-me.pdf', 'Keep Me', 'NEEDS_REVIEW', 'EXTRACTED', 123)");
            }

            DatabaseConfig.initializeSchemaIfMissing();

            try (Connection conn = DatabaseConfig.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT file_name FROM contracts WHERE file_name = 'keep-me.pdf'")) {
                assertTrue(rs.next());
                assertEquals("keep-me.pdf", rs.getString("file_name"));
            }

            Set<String> tables = new HashSet<>();
            try (Connection conn = DatabaseConfig.getConnection();
                 ResultSet rs = conn.getMetaData().getTables(conn.getCatalog(), null, "%", new String[]{"TABLE"})) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME").toLowerCase());
                }
            }

            assertTrue(tables.contains("contracts"));
            assertTrue(tables.contains("obligations"));
            assertTrue(tables.contains("activity_logs"));
        } finally {
            DatabaseConfig.shutdown();
        }
    }
}
