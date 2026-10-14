package com.contractiq.config;

import com.contractiq.util.EnvLoader;

import java.io.File;

public class AppConfig {
    public static final String APP_NAME = "ContractIQ";
    public static final String APP_VERSION = "1.0.0";

    public static String getUploadDir() {
        String uploadDir = EnvLoader.get("UPLOAD_DIR", "uploads");
        File dir = new File(uploadDir);
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return dir.getAbsolutePath();
    }

    public static long getMaxFileSizeBytes() {
        int mb = EnvLoader.getInt("MAX_FILE_SIZE_MB", 15);
        return (long) mb * 1024 * 1024;
    }

    public static String getLlmApiKey() {
        return EnvLoader.get("LLM_API_KEY", "");
    }

    public static String getLlmApiUrl() {
        return EnvLoader.get("LLM_API_URL", "https://api.openai.com/v1/chat/completions");
    }

    public static String getLlmModel() {
        return EnvLoader.get("LLM_MODEL", "gpt-4o-mini");
    }

    public static int getLlmTimeoutSeconds() {
        return EnvLoader.getInt("LLM_TIMEOUT_SECONDS", 60);
    }

    public static boolean isDemoMode() {
        return EnvLoader.getBoolean("DEMO_MODE", false);
    }

    public static boolean isProduction() {
        return "production".equalsIgnoreCase(EnvLoader.get("APP_ENV", "development"));
    }

    public static String getBasicAuthUsername() {
        return EnvLoader.get("APP_BASIC_AUTH_USERNAME", "");
    }

    public static String getBasicAuthPassword() {
        return EnvLoader.get("APP_BASIC_AUTH_PASSWORD", "");
    }

    public static boolean isBasicAuthConfigured() {
        return !getBasicAuthUsername().isBlank() && !getBasicAuthPassword().isBlank();
    }

    public static boolean isLlmConfigured() {
        String key = getLlmApiKey();
        return key != null && !key.trim().isEmpty() && !key.contains("your_");
    }
}
