package com.contractiq.util;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

public class SecurityUtil {

    /**
     * Sanitizes a file name to prevent path traversal attacks.
     */
    public static String sanitizeFileName(String originalName) {
        if (originalName == null || originalName.trim().isEmpty()) {
            return "uploaded_document_" + System.currentTimeMillis() + ".pdf";
        }
        // Extract base name only (strip any directory path)
        String baseName = originalName.replace('\\', '/');
        int lastSlash = baseName.lastIndexOf('/');
        if (lastSlash >= 0) {
            baseName = baseName.substring(lastSlash + 1);
        }
        // Remove control chars, path traversal sequences
        baseName = baseName.replaceAll("[^a-zA-Z0-9._\\-]", "_");
        if (baseName.isEmpty() || baseName.startsWith(".")) {
            baseName = "contract_" + System.currentTimeMillis() + ".pdf";
        }
        return baseName;
    }

    /**
     * Generates a unique, collision-resistant storage file name.
     */
    public static String generateStorageFileName(String sanitizedOriginalName) {
        String uuid = UUID.randomUUID().toString().substring(0, 8);
        long timestamp = System.currentTimeMillis();
        return timestamp + "_" + uuid + "_" + sanitizedOriginalName;
    }

    /**
     * Computes SHA-256 hash of byte array for duplicate detection and document integrity.
     */
    public static String computeSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * Basic HTML escaping to prevent XSS vulnerabilities when displaying user/extracted text.
     */
    public static String escapeHtml(String input) {
        if (input == null) return "";
        return input.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;")
                    .replace("'", "&#x27;");
    }

    /**
     * Redacts sensitive token or key for safe logging.
     */
    public static String redactApiKey(String apiKey) {
        if (apiKey == null || apiKey.length() < 8) return "***";
        return apiKey.substring(0, 3) + "..." + apiKey.substring(apiKey.length() - 3);
    }
}
