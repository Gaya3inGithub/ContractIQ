package com.contractiq.test;

import com.contractiq.config.AppConfig;
import com.contractiq.exception.StorageException;
import com.contractiq.storage.ContractStorageService;
import com.contractiq.storage.StoredFile;
import com.contractiq.util.EnvLoader;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class StorageServiceTest {
    private Map<String, String> originalEnvironment;

    @AfterEach
    void restoreEnvironment() throws Exception {
        if (originalEnvironment != null) {
            Map<String, String> environment = environment();
            environment.clear();
            environment.putAll(originalEnvironment);
        }
    }

    @Test
    void localStorageKeepsTheExistingDevelopmentWorkflow() throws Exception {
        configure("local", "uploads/test-local-storage", "", "", "", "");
        ContractStorageService storage = new ContractStorageService();

        StoredFile result = storage.saveUploadedFile("sample.pdf", new ByteArrayInputStream("test-data".getBytes(StandardCharsets.UTF_8)));

        assertNotNull(result);
        assertTrue(result.getStorageKey().contains("uploads"));
        assertFalse(ContractStorageService.isRemoteStorageKey(result.getStorageKey()));

        java.io.File localFile = new java.io.File(result.getStorageKey());
        if (localFile.exists()) {
            localFile.delete();
        }
    }

    @Test
    void objectStorageRequiresExplicitConfiguration() throws Exception {
        configure("object-store", "", "", "", "", "");

        StorageException ex = assertThrows(StorageException.class, () -> {
            new ContractStorageService().saveUploadedFile("sample.pdf", new ByteArrayInputStream("test-data".getBytes(StandardCharsets.UTF_8)));
        });

        assertTrue(ex.getMessage().contains("OBJECT_STORAGE_BUCKET") || ex.getMessage().contains("Object storage is enabled"));
    }

    private void configure(String storageProvider,
                           String uploadDir,
                           String bucket,
                           String endpoint,
                           String accessKey,
                           String secretKey) throws Exception {
        Map<String, String> environment = environment();
        originalEnvironment = new HashMap<>(environment);
        environment.put("STORAGE_PROVIDER", storageProvider);
        environment.put("UPLOAD_DIR", uploadDir);
        environment.put("OBJECT_STORAGE_BUCKET", bucket);
        environment.put("OBJECT_STORAGE_ENDPOINT", endpoint);
        environment.put("OBJECT_STORAGE_ACCESS_KEY_ID", accessKey);
        environment.put("OBJECT_STORAGE_SECRET_ACCESS_KEY", secretKey);
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> environment() throws Exception {
        Field field = EnvLoader.class.getDeclaredField("envMap");
        field.setAccessible(true);
        return (Map<String, String>) field.get(null);
    }
}
