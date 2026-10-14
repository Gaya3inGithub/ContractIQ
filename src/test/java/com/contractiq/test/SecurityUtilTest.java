package com.contractiq.test;

import com.contractiq.util.SecurityUtil;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class SecurityUtilTest {

    @Test
    void testSanitizeFileNamePathTraversal() {
        String input = "../../../etc/passwd.pdf";
        String clean = SecurityUtil.sanitizeFileName(input);
        assertFalse(clean.contains("/"));
        assertFalse(clean.contains("\\"));
        assertFalse(clean.contains(".."));
        assertTrue(clean.endsWith(".pdf"));
    }

    @Test
    void testSanitizeFileNameWindowsPath() {
        String input = "C:\\Users\\Admin\\Desktop\\Important Contract #1.pdf";
        String clean = SecurityUtil.sanitizeFileName(input);
        assertFalse(clean.contains(":"));
        assertFalse(clean.contains("\\"));
        assertTrue(clean.endsWith(".pdf"));
    }

    @Test
    void testSha256Computation() {
        byte[] data = "Hello ContractIQ".getBytes(StandardCharsets.UTF_8);
        String hash = SecurityUtil.computeSha256(data);
        assertNotNull(hash);
        assertEquals(64, hash.length()); // SHA-256 hex is 64 characters
    }

    @Test
    void testEscapeHtml() {
        String unsafe = "<script>alert('xss');</script> & \"test\"";
        String escaped = SecurityUtil.escapeHtml(unsafe);
        assertFalse(escaped.contains("<script>"));
        assertTrue(escaped.contains("&lt;script&gt;"));
        assertTrue(escaped.contains("&quot;"));
        assertTrue(escaped.contains("&amp;"));
    }

    @Test
    void testRedactApiKey() {
        String key = "sk-proj-1234567890abcdef";
        String redacted = SecurityUtil.redactApiKey(key);
        assertTrue(redacted.startsWith("sk-"));
        assertTrue(redacted.endsWith("def"));
        assertTrue(redacted.contains("..."));
        assertFalse(redacted.contains("1234567890"));
    }
}
