package com.contractiq.test;

import com.contractiq.pdf.ExtractedPdfContent;
import com.contractiq.pdf.PdfExtractionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class PdfExtractionServiceTest {

    private PdfExtractionService extractionService;

    @BeforeEach
    void setUp() {
        extractionService = new PdfExtractionService();
    }

    @Test
    void testExtractTextFromPdf(@TempDir File tempDir) throws IOException {
        File pdf = new File(tempDir, "sample_agreement.pdf");
        PdfTestGenerator.createSampleContractPdf(pdf);

        ExtractedPdfContent content = extractionService.extractText(pdf);

        assertNotNull(content);
        assertEquals(1, content.getPageCount());
        assertTrue(content.getCharacterCount() > 50);

        String text = content.getFullText();
        assertTrue(text.contains("ENTERPRISE CLOUD SERVICES AGREEMENT"));
        assertTrue(text.contains("CloudScale Systems Inc."));
        assertTrue(text.contains("Horizon Logistics Corp."));
        assertTrue(text.contains("Net 30 days"));

        assertNotNull(content.getPageTexts().get(1));
        assertTrue(content.getPageTexts().get(1).contains("Payment Terms"));
    }
}
