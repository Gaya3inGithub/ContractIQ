package com.contractiq.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Loads configuration from environment variables and falls back to a local .env file.
 */
public class EnvLoader {
    private static final Logger logger = LoggerFactory.getLogger(EnvLoader.class);
    private static final Map<String, String> envMap = new HashMap<>();

    static {
        loadDotEnv();
    }

    private static void loadDotEnv() {
        File envFile = new File(".env");
        if (!envFile.exists()) {
            // Check workspace root if running in nested container or servlet container
            String userDir = System.getProperty("user.dir");
            if (userDir != null) {
                File candidate = new File(userDir, ".env");
                if (candidate.exists()) {
                    envFile = candidate;
                }
            }
        }

        if (envFile.exists() && envFile.isFile()) {
            logger.info("Loading environment configurations from {}", envFile.getAbsolutePath());
            try (BufferedReader reader = new BufferedReader(new FileReader(envFile, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("#")) {
                        continue;
                    }
                    int eqIdx = line.indexOf('=');
                    if (eqIdx > 0) {
                        String key = line.substring(0, eqIdx).trim();
                        String value = line.substring(eqIdx + 1).trim();
                        // Strip quotes if present
                        if ((value.startsWith("\"") && value.endsWith("\"")) ||
                            (value.startsWith("'") && value.endsWith("'"))) {
                            value = value.substring(1, value.length() - 1);
                        }
                        envMap.put(key, value);
                    }
                }
            } catch (IOException e) {
                logger.warn("Could not read .env file: {}", e.getMessage());
            }
        }
    }

    public static String get(String key, String defaultValue) {
        String fileVal = envMap.get(key);
        if (fileVal != null && !fileVal.trim().isEmpty()) {
            return fileVal.trim();
        }
        String systemEnv = System.getenv(key);
        if (systemEnv != null && !systemEnv.trim().isEmpty()) {
            return systemEnv.trim();
        }
        return defaultValue;
    }

    public static String get(String key) {
        return get(key, null);
    }

    public static int getInt(String key, int defaultValue) {
        String val = get(key);
        if (val == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            logger.warn("Failed to parse integer for env {}: '{}', using default {}", key, val, defaultValue);
            return defaultValue;
        }
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String val = get(key);
        if (val == null) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(val.trim()) || "1".equals(val.trim());
    }
}
