package com.contractiq.pdf;

import com.contractiq.exception.ValidationException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class PdfExtractionService {
    private static final Logger logger = LoggerFactory.getLogger(PdfExtractionService.class);
    private static final int MAX_EXTRACTED_CHARS = 120_000; // Limit for LLM context safety

    public ExtractedPdfContent extractText(File pdfFile) {
        if (pdfFile == null || !pdfFile.exists()) {
            throw new ValidationException("PDF file does not exist on disk for text extraction.");
        }

        logger.info("Extracting text from PDF: {} ({} bytes)", pdfFile.getName(), pdfFile.length());

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            if (document.isEncrypted()) {
                throw new ValidationException("Cannot process password-protected or encrypted PDF documents. Please upload an unencrypted file.");
            }

            int numberOfPages = document.getNumberOfPages();
            if (numberOfPages == 0) {
                throw new ValidationException("PDF document contains no pages.");
            }

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);

            String fullText = stripper.getText(document);
            if (fullText == null || fullText.trim().isEmpty()) {
                logger.warn("Extracted text from PDF is empty. Document might be image-scanned.");
                fullText = "[Scanned or image-based PDF. Text could not be parsed via direct text stripping.]";
            } else if (fullText.length() > MAX_EXTRACTED_CHARS) {
                logger.info("Truncating extracted text from {} chars to {} chars for safety.", fullText.length(), MAX_EXTRACTED_CHARS);
                fullText = fullText.substring(0, MAX_EXTRACTED_CHARS);
            }

            // Extract per-page mapping
            Map<Integer, String> pageTexts = new HashMap<>();
            for (int page = 1; page <= Math.min(numberOfPages, 50); page++) {
                try {
                    stripper.setStartPage(page);
                    stripper.setEndPage(page);
                    String pageText = stripper.getText(document);
                    pageTexts.put(page, pageText != null ? pageText.trim() : "");
                } catch (Exception e) {
                    logger.debug("Failed to extract individual page {}: {}", page, e.getMessage());
                }
            }

            logger.info("Successfully extracted text from {}: {} pages, {} characters.",
                    pdfFile.getName(), numberOfPages, fullText.length());

            return new ExtractedPdfContent(fullText, numberOfPages, pageTexts);

        } catch (IOException e) {
            logger.error("IOException while extracting text from PDF {}: {}", pdfFile.getName(), e.getMessage());
            throw new ValidationException("Failed to read or parse PDF contents: " + e.getMessage());
        }
    }
}
