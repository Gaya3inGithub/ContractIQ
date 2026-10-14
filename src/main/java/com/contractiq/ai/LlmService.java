package com.contractiq.ai;

import com.contractiq.pdf.ExtractedPdfContent;

public interface LlmService {
    /**
     * Analyzes contract text and returns structured extraction suggestions.
     *
     * @param fileName Original file name
     * @param pdfContent Extracted text and page mapping
     * @return Structured AI extraction result
     */
    LlmExtractionResult extractContractObligations(String fileName, ExtractedPdfContent pdfContent);

    /**
     * Checks if real LLM credentials are configured and available.
     */
    boolean isConfigured();

    /**
     * Returns the name/id of the underlying model or provider.
     */
    String getProviderName();
}
