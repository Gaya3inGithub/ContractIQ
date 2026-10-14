package com.contractiq.config;

import com.contractiq.util.EnvLoader;

import java.io.File;
import java.util.Locale;

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

    public static String getStorageProvider() {
        return EnvLoader.get("STORAGE_PROVIDER", "local").trim().toLowerCase(Locale.ROOT);
    }

    public static boolean isLocalStorageEnabled() {
        String provider = getStorageProvider();
        return provider.isBlank() || provider.equals("local") || provider.equals("filesystem");
    }

    public static boolean isObjectStorageEnabled() {
        String provider = getStorageProvider();
        return provider.equals("object-store") || provider.equals("s3") || provider.equals("r2") || provider.equals("cloudflare-r2");
    }

    public static String getObjectStorageBucket() {
        return EnvLoader.get("OBJECT_STORAGE_BUCKET", "");
    }

    public static String getObjectStorageEndpoint() {
        return EnvLoader.get("OBJECT_STORAGE_ENDPOINT", "");
    }

    public static String getObjectStorageRegion() {
        return EnvLoader.get("OBJECT_STORAGE_REGION", "auto");
    }

    public static String getObjectStorageAccessKeyId() {
        return EnvLoader.get("OBJECT_STORAGE_ACCESS_KEY_ID", "");
    }

    public static String getObjectStorageSecretAccessKey() {
        return EnvLoader.get("OBJECT_STORAGE_SECRET_ACCESS_KEY", "");
    }

    public static boolean isObjectStorageConfigured() {
        return isObjectStorageEnabled()
                && !getObjectStorageBucket().isBlank()
                && !getObjectStorageEndpoint().isBlank()
                && !getObjectStorageAccessKeyId().isBlank()
                && !getObjectStorageSecretAccessKey().isBlank();
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
