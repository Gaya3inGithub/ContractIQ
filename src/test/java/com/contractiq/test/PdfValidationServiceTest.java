package com.contractiq.test;

import com.contractiq.exception.ValidationException;
import com.contractiq.pdf.PdfValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class PdfValidationServiceTest {

    private PdfValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new PdfValidationService();
    }

    @Test
    void testValidPdfAccepted(@TempDir File tempDir) throws IOException {
        File pdf = new File(tempDir, "contract.pdf");
        PdfTestGenerator.createSampleContractPdf(pdf);

        assertDoesNotThrow(() -> validationService.validatePdfFile(pdf, "contract.pdf"));
    }

    @Test
    void testEmptyFileRejected(@TempDir File tempDir) throws IOException {
        File emptyFile = new File(tempDir, "empty.pdf");
        assertTrue(emptyFile.createNewFile());

        ValidationException ex = assertThrows(ValidationException.class, () ->
                validationService.validatePdfFile(emptyFile, "empty.pdf"));
        assertTrue(ex.getMessage().contains("empty"));
    }

    @Test
    void testNonPdfExtensionRejected(@TempDir File tempDir) throws IOException {
        File textFile = new File(tempDir, "contract.docx");
        try (FileOutputStream fos = new FileOutputStream(textFile)) {
            fos.write("Not a PDF".getBytes(StandardCharsets.UTF_8));
        }

        ValidationException ex = assertThrows(ValidationException.class, () ->
                validationService.validatePdfFile(textFile, "contract.docx"));
        assertTrue(ex.getMessage().contains("Only PDF documents"));
    }

    @Test
    void testFakePdfHeaderRejected(@TempDir File tempDir) throws IOException {
        File fakePdf = new File(tempDir, "malicious.pdf");
        try (FileOutputStream fos = new FileOutputStream(fakePdf)) {
            fos.write("THIS_IS_NOT_A_REAL_PDF_HEADER".getBytes(StandardCharsets.UTF_8));
        }

        ValidationException ex = assertThrows(ValidationException.class, () ->
                validationService.validatePdfFile(fakePdf, "malicious.pdf"));
        assertTrue(ex.getMessage().contains("standard PDF specifications"));
    }
}
