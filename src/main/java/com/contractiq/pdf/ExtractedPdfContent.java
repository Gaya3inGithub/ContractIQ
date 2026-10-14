package com.contractiq.pdf;

import java.util.Collections;
import java.util.Map;

public class ExtractedPdfContent {
    private final String fullText;
    private final int pageCount;
    private final Map<Integer, String> pageTexts;
    private final int characterCount;

    public ExtractedPdfContent(String fullText, int pageCount, Map<Integer, String> pageTexts) {
        this.fullText = fullText != null ? fullText : "";
        this.pageCount = pageCount;
        this.pageTexts = pageTexts != null ? pageTexts : Collections.emptyMap();
        this.characterCount = this.fullText.length();
    }

    public String getFullText() { return fullText; }
    public int getPageCount() { return pageCount; }
    public Map<Integer, String> getPageTexts() { return pageTexts; }
    public int getCharacterCount() { return characterCount; }

    public String getExcerpt(int maxLength) {
        if (fullText.length() <= maxLength) {
            return fullText;
        }
        return fullText.substring(0, maxLength) + "... [truncated]";
    }
}
