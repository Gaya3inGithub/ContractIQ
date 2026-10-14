package com.contractiq.pdf;

import com.contractiq.config.AppConfig;
import com.contractiq.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class PdfValidationService {
    private static final Logger logger = LoggerFactory.getLogger(PdfValidationService.class);
    private static final byte[] PDF_MAGIC_BYTES = new byte[]{0x25, 0x50, 0x44, 0x46, 0x2D}; // %PDF-

    public void validatePdfFile(File file, String originalFileName) {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new ValidationException("Uploaded file does not exist on disk.");
        }

        long length = file.length();
        if (length == 0) {
            throw new ValidationException("Uploaded file is empty (0 bytes).");
        }

        long maxSize = AppConfig.getMaxFileSizeBytes();
        if (length > maxSize) {
            double mb = (double) length / (1024 * 1024);
            double limitMb = (double) maxSize / (1024 * 1024);
            throw new ValidationException(String.format("File size (%.2f MB) exceeds maximum allowed limit of %.0f MB.", mb, limitMb));
        }

        if (originalFileName == null || !originalFileName.toLowerCase().endsWith(".pdf")) {
            throw new ValidationException("Invalid file type. Only PDF documents (.pdf) are permitted.");
        }

        // Validate PDF magic bytes header
        try (FileInputStream fis = new FileInputStream(file)) {
            byte[] header = new byte[5];
            int read = fis.read(header);
            if (read < 5) {
                throw new ValidationException("File is corrupted or too small to be a valid PDF document.");
            }
            for (int i = 0; i < 5; i++) {
                if (header[i] != PDF_MAGIC_BYTES[i]) {
                    throw new ValidationException("Invalid PDF format. The file header does not match standard PDF specifications (%PDF-).");
                }
            }
        } catch (IOException e) {
            logger.error("Error reading PDF header: {}", e.getMessage());
            throw new ValidationException("Failed to read and validate the PDF document: " + e.getMessage());
        }

        logger.info("PDF file validated successfully: {} ({} bytes)", originalFileName, length);
    }
}
