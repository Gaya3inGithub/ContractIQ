package com.contractiq.test;

import com.contractiq.controller.ContractUploadServlet;
import jakarta.servlet.annotation.MultipartConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ContractUploadConfigurationTest {

    @Test
    void multipartLimitsMatchTheDocumentedFifteenMegabyteDefault() {
        MultipartConfig config = ContractUploadServlet.class.getAnnotation(MultipartConfig.class);

        assertNotNull(config);
        assertEquals(15L * 1024 * 1024, config.maxFileSize());
        assertEquals(16L * 1024 * 1024, config.maxRequestSize());
    }
}
